(ns zetawar.game
  (:require
   [com.rpl.specter :refer [ALL LAST collect-one filterer selected?]]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.data :as data]
   [zetawar.db :refer [e find-by qe qes qess]]
   [zetawar.hex :as hex]
   [zetawar.util :refer [breakpoint inspect oonly]])
  (:require-macros
   [com.rpl.specter.macros :refer [select setval transform]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Util

(def game-pos-idx
  (memoize
    (fn game-pos-idx [game q r]
      (if (and game q r)
        (+ r (* 1000 (+ (* (e game) 1000) q)))
        -1))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game

(defn game-by-id [db game-id]
  (find-by db :game/id game-id))

(defn current-faction-color [game]
  (get-in game [:game/current-faction :faction/color]))

(defn next-faction-color [game]
  (get-in game [:game/current-faction :faction/next-faction :faction/color]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Factions

(defn to-faction-color [color]
  (->> color
       name
       (keyword 'faction.color)))

(defn faction-by-color [db game color]
  (qe '[:find ?f
        :in $ ?g ?color
        :where
        [?g :game/factions ?f]
        [?f :faction/color ?color]]
      db (e game) (to-faction-color color)))

(defn faction-bases [db faction]
  (qess '[:find ?t
          :in $ ?f
          :where
          [?t :terrain/owner ?f]]
        db (e faction)))

(defn faction-base-count [db faction]
  (-> (d/q '[:find (count ?b)
             :in $ ?f
             :where
             [?b :terrain/owner ?f]]
           db (e faction))
      ffirst
      (or 0)))

(defn enemy-base-count [db faction]
  (-> (d/q '[:find (count ?b)
             :in $ ?f
             :where
             [?b :terrain/owner ?ef]
             [(not= ?ef ?f)]]
           db (e faction))
      ffirst
      (or 0)))

(defn faction-unit-count [db faction]
  (-> (d/q '[:find (count ?u)
             :in $ ?f
             :where
             [?f :faction/units ?u]]
           db (e faction))
      ffirst
      (or 0)))

(defn enemy-unit-count [db faction]
  (-> (d/q '[:find (count ?u)
             :in $ ?f
             :where
             [?f :faction/units ?u]
             [(not= ?f ?cf)]]
           db (e faction))
      ffirst
      (or 0)))

(defn income [db game faction]
  (let [base-count (faction-base-count db faction)
        {:keys [game/credits-per-base]} game]
    (* base-count credits-per-base)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Terrain

(defn terrain-type-by-short-id [db short-id]
  (find-by db
           :terrain-type/id
           (keyword 'terrain-type.id (name short-id))))

(defn terrain? [x]
  (contains? x :terrain/type))

(defn base? [x]
  (= :terrain-type.id/base
     (get-in x [:terrain/type :terrain-type/id])))

(defn terrain-hex [terrain]
  [(:terrain/q terrain)
   (:terrain/r terrain)])

(defn terrain-at [db game q r]
  (->> (game-pos-idx game q r)
       (d/datoms db :avet :terrain/game-pos-idx)
       first
       :e
       (d/entity db)))

(defn checked-terrain-at [db game q r]
  (let [terrain (terrain-at db game q r)]
    (when-not terrain
      (throw (ex-info "No terrain at specified coordinates"
                      {:q q :r r})))
    terrain))

(defn base-at [db game q r]
  (qe '[:find ?t
        :in $ ?idx
        :where
        [?t :terrain/game-pos-idx ?idx]
        [?t :terrain/type ?tt]
        [?tt :terrain-type/id :terrain-type.id/base]]
      db (game-pos-idx game q r)))

(defn checked-base-at [db game q r]
  (let [base (base-at db game q r)]
    (when-not base
      (throw (ex-info "No base at specified coordinates"
                      {:q q :r r})))
    base))

(defn check-base-current [db game base]
  (let [cur-faction (:game/current-faction game)
        base-faction (qe '[:find ?f
                           :in $ ?b
                           :where
                           [?b :terrain/owner ?f]]
                         db (e base))]
    (when (not= cur-faction base-faction)
      (throw (ex-info "Base is not owned by the current faction"
                      {:current-faction (:faction/color cur-faction)
                       :base-faction (:faction/color base-faction)})))))

(defn current-base? [db game x]
  (when (base? x)
    (let [cur-faction (:game/current-faction game)
          base-faction (qe '[:find ?f
                             :in $ ?t
                             :where
                             [?t :terrain/owner ?f]]
                           db (e x))]
      (= cur-faction base-faction))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Units

(defn to-unit-type-id [unit-type]
  (->> unit-type
       name
       (keyword 'unit-type.id)))

(defn unit? [x]
  (contains? x :unit/type))

(defn unit-hex [unit]
  [(:unit/q unit)
   (:unit/r unit)])

(defn unit-at [db game q r]
  (->> (game-pos-idx game q r)
       (d/datoms db :avet :unit/game-pos-idx)
       first
       :e
       (d/entity db)))

(defn checked-unit-at [db game q r]
  (let [unit (unit-at db game q r)]
    (when-not unit
      (throw (ex-info "Unit does not exist at specified coordinates"
                      {:q q :r r})))
    unit))

(defn unit-terrain [db game unit]
  (let [{:keys [unit/q unit/r]} unit]
    (terrain-at db game q r)))

(defn unit-faction [db unit]
  (if (:faction/_units unit)
    (:faction/_units unit)
    (find-by db :faction/units (e unit))))

(defn check-unit-current [db game unit]
  (let [cur-faction (:game/current-faction game)
        u-faction (unit-faction db unit)]
    (when (not= (e cur-faction) (e u-faction))
      (throw (ex-info "Unit is not a member of the current faction"
                      {:current-faction (:faction/color cur-faction)
                       :unit-faction (:faction/color u-faction)})))))

(defn unit-current? [db game unit]
  (try
    (check-unit-current db game unit)
    true
    (catch :default ex
      false)))

(defn on-base? [db game unit]
  (base? (terrain-at db game (:unit/q unit) (:unit/r unit))))

(defn on-capturable-base? [db game unit]
  (let [{:keys [unit/q unit/r]} unit
        terrain (terrain-at db game q r)]
    (and (base? terrain)
         (not= (:terrain/owner terrain)
               (unit-faction db unit)))))

(defn unit-ex [message unit]
  (let [{:keys [unit/q unit/r]} unit]
    (ex-info message {:q q
                      :r r})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Unit States

(defn next-state [unit action]
  (let [transition-map (->> unit
                            :unit/current-state
                            :unit/transitions
                            (map (juxt :unit-state-transition/action-type
                                       :unit-state-transition/new-state))
                            action)]
    (transition-map action)))

(defn checked-next-state [unit action]
  (let [new-state (next-state unit action)]
    (when-not new-state
      (throw (ex-info "No state transition from current state found for action"
                      {:current-state (get-in unit [:unit/current-state :unit-state/id])
                       :action action})))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Movement

;; TODO: make not being able to walk through your own units a game option
;; TODO: return moves as ({:from [q r] :to [q r] :path [[q r] [q r] ...] :cost n})
(defn valid-moves [db game unit]
  (let [start [(:unit/q unit) (:unit/r unit)]
        u-faction (e (unit-faction db unit))
        unit-type (e (:unit/type unit))
        unit-at (memoize #(unit-at db game %1 %2))
        adjacent-idxs (memoize (fn adjacent-idxs [q r]
                                 (mapv #(apply game-pos-idx game %) (hex/adjacents q r))))
        terrain-type->cost (into {}
                                 (d/q '[:find ?tt ?mc
                                        :in $ ?ut
                                        :where
                                        [?e :terrain-effect/terrain-type ?tt]
                                        [?e :terrain-effect/unit-type ?ut]
                                        [?e :terrain-effect/movement-cost ?mc]]
                                      db unit-type))
        ;; TODO: move movment-cost into query
        adjacent-costs (memoize (fn adjacent-costs [q r]
                                  (sequence (comp (map (fn [[q r tt]]
                                                         (when-let [cost (terrain-type->cost tt)]
                                                           [q r (terrain-type->cost tt)])))
                                                  (remove nil?))
                                            (d/q '[:find ?q ?r ?tt
                                                   :in $ [?idx ...]
                                                   :where
                                                   [?t :terrain/game-pos-idx ?idx]
                                                   [?t :terrain/q ?q]
                                                   [?t :terrain/r ?r]
                                                   [?t :terrain/type ?tt]]
                                                 db (adjacent-idxs q r)))))
        adjacent-enemy? (memoize (fn adjacent-enemy? [q r]
                                   (transduce
                                    (comp (map #(-> (d/datoms db :avet :unit/game-pos-idx %) first :e))
                                          (remove nil?)
                                          (map #(-> (d/datoms db :avet :faction/units %) first :e))
                                          (map #(not= u-faction %)))
                                    #(or %1 %2)
                                    false
                                    (adjacent-idxs q r))))
        ;; TODO: rename moves arg to move-costs
        ;; TODO: track move costs instead of remaining movement
        expand-frontier (fn [frontier moves]
                          (loop [[[[q r] movement] & remaining-frontier] (seq frontier) new-frontier {}]
                            (if movement
                              (let [costs (adjacent-costs q r)
                                    ;; TODO: simplify
                                    new-moves (into
                                               {}
                                               (comp
                                                (map (fn [[q r cost]]
                                                       ;; moving into zone of control depletes movement
                                                       (if (adjacent-enemy? q r)
                                                         [q r (max movement cost)]
                                                         [q r cost])))
                                                (remove nil?)
                                                (map (fn [[q r cost]]
                                                       (let [new-movement (- movement cost)]
                                                         (when (and (> new-movement (get moves [q r] -1))
                                                                    (> new-movement (get frontier [q r] -1))
                                                                    (> new-movement (get new-frontier [q r] -1)))
                                                           [[q r] new-movement]))))
                                                (remove nil?))
                                               costs)]
                                (recur remaining-frontier (conj new-frontier new-moves)))
                              new-frontier)))]
    (loop [frontier {start (get-in unit [:unit/type :unit-type/movement])} moves {}]
      (let [new-frontier (expand-frontier frontier moves)
            new-moves (conj moves frontier)]
        (if (empty? new-frontier)
          ;; TODO: simplify + use transducer
          (->> (dissoc new-moves start)
               (map first)
               (remove #(apply unit-at %)) ; remove occupied locations
               (map (fn [dest] {:from start :to dest}))
               (into #{}))
          (recur new-frontier new-moves))))))

;; TODO: implement valid-move?

(defn valid-destinations [db game unit]
  (into #{}
        (map :to)
        (valid-moves db game unit)))

(defn valid-destination? [db game unit q r]
  (contains? (valid-destinations db game unit) [q r]))

(defn check-valid-destination [db game unit q r]
  (when-not (valid-destination? db game unit q r)
    (throw (ex-info "Specified destination is not a valid move"
                    {:q q :r r}))))

(defn check-can-move [db game unit]
  (check-unit-current db game unit)
  (when (= (:unit/round-built unit) (:game/round game))
    (throw (unit-ex "Unit cannot move on the turn it was built" unit)))
  (when (> (:unit/move-count unit) 0)
    (throw (unit-ex "Unit can only move once per turn" unit)))
  (when (> (:unit/attack-count unit) 0)
    (throw (unit-ex "Unit cannot move after attacking" unit)))
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot move while capturing" unit)))
  (when (:unit/repaired unit)
    (throw (unit-ex "Unit cannot move after repair" unit))))

(defn can-move? [db game unit]
  (try
    (check-can-move db game unit)
    true
    (catch :default ex
      false)))

;; Get moves
;; - get adjacent coordinates
;; - get move costs
;; - add legal moves to frontier
;; - repeat with updated movement points + frontier

(defn teleport-tx [db game from-q from-r to-q to-r]
  (let [unit (checked-unit-at db game from-q from-r)]
    [{:db/id (e unit)
      :unit/game-pos-idx (game-pos-idx game to-q to-r)
      :unit/q to-q
      :unit/r to-r}]))

;; TODO: check move is valid
(defn move-tx
  "Returns a transaction that updates the unit's location and move count."
  ([db game unit to-terrain]
   (let [new-move-count (inc (get unit :unit/move-count 0))
         [to-q to-r] (terrain-hex to-terrain)]
     (check-can-move db game unit)
     [{:db/id (e unit)
       :unit/game-pos-idx (game-pos-idx game to-q to-r)
       :unit/q to-q
       :unit/r to-r
       :unit/move-count new-move-count}]))
  ([db game from-q from-r to-q to-r]
   (let [unit (checked-unit-at db game from-q from-r)
         terrain (checked-terrain-at db game to-q to-r)]
     (move-tx db game unit terrain))))

(defn move! [conn game-id from-q from-r to-q to-r]
  (let [db @conn
        game (game-by-id db game-id)]
    (d/transact! conn (move-tx db game from-q from-r to-q to-r))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Attack

(defn check-can-attack [db game unit]
  (check-unit-current db game unit)
  (when (= (:unit/round-built unit) (:game/round game))
    (throw (unit-ex "Unit cannot attack on the turn it was built" unit)))
  (when (> (:unit/attack-count unit) 0)
    (throw (unit-ex "Unit can only attack once per turn" unit)))
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot attack while capturing" unit)))
  (when (:unit/repaired unit)
    (throw (unit-ex "Unit cannot attack after repair" unit))))

(defn can-attack? [db game unit]
  (try
    (check-can-attack db game unit)
    true
    (catch :default ex
      false)))

(defn check-in-range [db attacker defender]
  (let [distance (hex/distance (:unit/q attacker) (:unit/r attacker)
                               (:unit/q defender) (:unit/r defender))
        min-range (get-in attacker [:unit/type :unit-type/min-range])
        max-range (get-in attacker [:unit/type :unit-type/max-range])]
    (when (or (< distance min-range)
              (> distance max-range))
      ;; TODO: add defender details to exception
      (throw (unit-ex "Targeted unit is not in range" attacker)))))

(defn in-range? [db attacker defender]
  (try
    (check-in-range db attacker defender)
    true
    (catch :default ex
      false)))

;; TODO: add bonuses for flanking, etc.
(defn attack-damage [db attacker defender attacker-terrain defender-terrain]
  (let [defender-armor-type (get-in defender [:unit/type :unit-type/armor-type])
        attack-strength (oonly (d/q '[:find ?s
                                      :in $ ?u ?at
                                      :where
                                      [?u  :unit/type ?ut]
                                      [?as :unit-strength/unit-type ?ut]
                                      [?as :unit-strength/armor-type ?at]
                                      [?as :unit-strength/attack ?s]]
                                    db (e attacker) defender-armor-type))
        armor (if (:unit/capturing defender)
                (get-in defender [:unit/type :unit-type/capturing-armor])
                (get-in defender [:unit/type :unit-type/armor]))
        attack-bonus (oonly (d/q '[:find ?a
                                   :in $ ?u ?t
                                   :where
                                   [?u :unit/type ?ut]
                                   [?t :terrain/type ?tt]
                                   [?e :terrain-effect/terrain-type ?tt]
                                   [?e :terrain-effect/unit-type ?ut]
                                   [?e :terrain-effect/attack-bonus ?a]]
                                 db (e attacker) (e attacker-terrain)))
        armor-bonus (oonly (d/q '[:find ?d
                                  :in $ ?u ?t
                                  :where
                                  [?u :unit/type ?ut]
                                  [?t :terrain/type ?tt]
                                  [?e :terrain-effect/terrain-type ?tt]
                                  [?e :terrain-effect/unit-type ?ut]
                                  [?e :terrain-effect/armor-bonus ?d]]
                                db (e defender) (e defender-terrain)))]
    (js/Math.round
     (max 0 (* (:unit/count attacker)
               (+ 0.5 (* 0.05 (+ (- (+ attack-strength attack-bonus)
                                    (+ armor armor-bonus))
                                 (:unit/attacked-count defender)))))))))

(defn battle-damage
  ([db game attacker defender]
   (let [attacker-terrain (terrain-at db game (:unit/q attacker) (:unit/r attacker))
         defender-terrain (terrain-at db game (:unit/q defender) (:unit/r defender))
         attack-count (get attacker :unit/attack-count 0)
         defender-damage (attack-damage db attacker defender attacker-terrain defender-terrain)
         attacker-damage (if (in-range? db defender attacker)
                           (attack-damage db defender attacker defender-terrain attacker-terrain)
                           0)]
     [(min (:unit/count attacker) attacker-damage)
      (min (:unit/count defender) defender-damage)]))
  ([db game attacker-q attacker-r defender-q defender-r]
   (let [attacker (checked-unit-at db game attacker-q attacker-r)
         defender (checked-unit-at db game defender-q defender-r)]
     (battle-damage db game attacker defender))))

;; TODO: add attacked unit to unit/attacked-units
(defn battle-tx
  ([db game attacker defender attacker-damage defender-damage]
   (check-can-attack db game attacker)
   (check-in-range db attacker defender)
   (let [attacker-terrain (terrain-at db game (:unit/q attacker) (:unit/r attacker))
         defender-terrain (terrain-at db game (:unit/q defender) (:unit/r defender))
         attack-count (get attacker :unit/attack-count 0)
         attacker-count (:unit/count attacker)
         defender-count (:unit/count defender)]
     (cond-> []
       (> defender-count defender-damage)
       (conj {:db/id (e defender)
              :unit/count (- defender-count defender-damage)
              :unit/attacked-count (inc (:unit/attacked-count defender))})

       (= defender-count defender-damage)
       (conj [:db.fn/retractEntity (e defender)])

       (> attacker-count attacker-damage)
       (conj {:db/id (e attacker)
              :unit/count (- attacker-count attacker-damage)
              :unit/attack-count (inc attack-count)})

       (= attacker-count attacker-damage)
       (conj [:db.fn/retractEntity (e attacker)]))))
  ([db game attacker-q attacker-r defender-q defender-r attacker-damage defender-damage]
   (let [attacker (checked-unit-at db game attacker-q attacker-r)
         defender (checked-unit-at db game defender-q defender-r)]
     (battle-tx db game attacker defender attacker-damage defender-damage))))

;; TODO: remove attack-tx (superceded by battle-tx)
(defn attack-tx
  ([db game attacker defender]
   (check-can-attack db game attacker)
   (check-in-range db attacker defender)
   (apply battle-tx db game attacker defender (battle-damage db game attacker defender)))
  ([db game attacker-q attacker-r defender-q defender-r]
   (let [attacker (checked-unit-at db game attacker-q attacker-r)
         defender (checked-unit-at db game defender-q defender-r)]
     (attack-tx db game attacker defender))))

(defn attack! [conn game-id attacker-q attacker-r defender-q defender-r]
  (let [db @conn
        game (game-by-id db game-id)
        tx (attack-tx db game attacker-q attacker-r defender-q defender-r)]
    (d/transact! conn tx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Repair

(defn check-can-repair [db game unit]
  (check-unit-current db game unit)
  (when (>= (:unit/count unit) (:game/max-unit-count game))
    (throw (unit-ex "Unit is already fully repaired" unit)))
  (when (:unit/repaired unit)
    (throw (unit-ex "Unit can only be repaired once per turn" unit)))
  (when (> (:unit/attack-count unit) 0)
    (throw (unit-ex "Unit cannot be repaired after attacking" unit)))
  (when (> (:unit/move-count unit) 0)
    (throw (unit-ex "Unit cannot be repaired after moving" unit)))
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot be repaired while capturing" unit))))

(defn can-repair? [db game unit]
  (try
    (check-can-repair db game unit)
    true
    (catch :default ex
      false)))

(defn repair-tx
  "Returns a transaction that increments unit count and sets the unit repaired
  flag to true."
  ([db game unit]
   (check-can-repair db game unit)
   [{:db/id (e unit)
     :unit/count (min (:game/max-unit-count game)
                      (+ (:unit/count unit)
                         (get-in unit [:unit/type :unit-type/repair])))
     :unit/repaired true}])
  ([db game q r]
   (let [unit (checked-unit-at db game q r)]
     (repair-tx db game unit))))

(defn repair! [conn game-id q r]
  (let [db @conn
        game (game-by-id db game-id)
        tx (repair-tx db game q r)]
    (d/transact! conn tx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Capture

(defn check-capturable [db game unit terrain]
  (check-unit-current db game unit)
  (when-not (-> unit :unit/type :unit-type/can-capture)
    (throw (unit-ex "Unit does not have the ability to capture" unit)))
  (when-not (and terrain (base? terrain))
    (throw (unit-ex "Unit unit is not on a base" unit)))
  (when (> (:unit/attack-count unit) 0)
    (throw (unit-ex "Unit cannot capture after attacking" unit)))
  (when (:unit/repaired unit)
    (throw (unit-ex "Unit cannot capture after being repaired" unit)))
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit is already caturing" unit)))
  (when (= (e (unit-faction db unit)) (some-> terrain :terrain/owner e))
    ;; TODO: add more exception info
    (throw (ex-info "Base is already owned by current faction"
                    {:x (:terrain/x terrain)
                     :y (:terrain/y terrain)}))))

(defn can-capture? [db game unit terrain]
  (try
    (check-capturable db game unit terrain)
    true
    (catch :default ex
      false)))


(defn capture-tx
  "Returns a transaction that sets the unit capturing flag and capture round."
  ([db game unit]
   (let [base (base-at db game (:unit/q unit) (:unit/r unit))
         round (:game/round game)]
     (check-capturable db game unit base)
     [{:db/id (e unit)
       :unit/capturing true
       :unit/capture-round (inc round)}]))
  ([db game q r]
   (let [unit (checked-unit-at db game q r)]
     (capture-tx db game unit))))

(defn capture! [conn game-id q r]
  (let [db @conn
        game (game-by-id db game-id)
        tx (capture-tx db game q r)]
    (d/transact! conn tx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Build

;; TODO: implement check-can-build

(defn check-unoccupied [db game q r]
  (let [unit (unit-at db game q r)]
    (when unit
      ;; TODO: include info about occupying unit
      (throw (ex-info "Base is occupied" {:q q :r r})))))

(defn unoccupied? [db game q r]
  (try
    (check-unoccupied db game q r)
    true
    (catch :default ex
      false)))

;; TODO: set unit/state
(defn build-tx
  "Returns a transaction that creates a new unit and updates faction credits."
  ([db game q r unit-type-id]
   (let [base (checked-base-at db game q r)]
     (build-tx db game base unit-type-id)))
  ([db game base unit-type-id]
   (let [unit-type (find-by db :unit-type/id unit-type-id)
         cur-faction (:game/current-faction game)
         credits (:faction/credits cur-faction)
         cost (:unit-type/cost unit-type)
         base-q (:terrain/q base)
         base-r (:terrain/r base)]
     (check-base-current db game base)
     (check-unoccupied db game base-q base-r)
     (when (> cost credits)
       (throw (ex-info "Unit cost exceeds available credits"
                       {:credits credits
                        :cost cost})))
     [{:db/id -1
       :unit/game-pos-idx (game-pos-idx game base-q base-r)
       :unit/q base-q
       :unit/r base-r
       :unit/round-built (:game/round game)
       :unit/type (e unit-type)
       :unit/count (:game/max-unit-count game)
       :unit/move-count 0
       :unit/attack-count 0
       :unit/attacked-count 0
       :unit/repaired false
       :unit/capturing false}
      {:db/id (e cur-faction)
       :faction/credits (- credits cost)
       :faction/units -1}])))

(defn build! [conn game-id q r unit-type-id]
  (let [db @conn
        game (game-by-id db game-id)
        tx (build-tx db game q r unit-type-id)]
    (d/transact! conn tx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; End Turn

(defn end-turn-capture-tx [db game unit]
  (let [{:keys [unit/capture-round unit/q unit/r]} unit
        faction (unit-faction db unit)
        terrain (checked-base-at db game q r)]
    [[:db/add (e terrain) :terrain/owner (e faction)]
     [:db.fn/retractEntity (e unit)]]))

(defn unit-end-turn-tx [db game unit]
  (let [{:keys [unit/capturing unit/capture-round]} unit]
    (cond-> [[:db/add (e unit) :unit/repaired false]
             [:db/add (e unit) :unit/move-count 0]
             [:db/add (e unit) :unit/attack-count 0]
             [:db/add (e unit) :unit/attacked-count 0]]
      (and capturing (= capture-round (:game/round game)))
      (into (end-turn-capture-tx db game unit)))))

(defn end-turn-tx
  "Return a transaction that completes captures, clears per round unit flags,
  updates the current faction, adds faction credits, and updates the round."
  [db game]
  (let [starting-faction (:game/starting-faction game)
        cur-faction (:game/current-faction game)
        next-faction (:faction/next-faction cur-faction)
        credits (+ (:faction/credits next-faction) (income db game next-faction))
        cur-round (:game/round game)
        new-round (if (= starting-faction next-faction)
                    (inc cur-round)
                    cur-round)
        units (:faction/units cur-faction)]
    (into [{:db/id (e next-faction)
            :faction/credits credits}
           {:db/id (e game)
            :game/round new-round
            :game/current-faction (e next-faction)}]
          (mapcat #(unit-end-turn-tx db game %) units))))

(defn end-turn! [conn game-id]
  (let [db @conn
        game (game-by-id db game-id)]
    (d/transact! conn (end-turn-tx db game))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Actions

(defn action-tx [db game action]
  (case (:action/type action)
    :action.type/build-unit
    (let [{:keys [action/q action/r action/unit-type-id]} action]
      (build-tx db game q r unit-type-id))

    :action.type/move-unit
    (let [{:keys [action/from-q action/from-r
                  action/to-q action/to-r]} action]
      (move-tx db game from-q from-r to-q to-r))

    :action.type/attack-unit
    (let [{:keys [action/attacker-q action/attacker-r
                  action/defender-q action/defender-r
                  action/attacker-damage
                  action/defender-damage]} action]
      (battle-tx db game
                 attacker-q attacker-r
                 defender-q defender-r
                 attacker-damage
                 defender-damage))

    :action.type/repair-unit
    (let [{:keys [action/q action/r]} action]
      (repair-tx db game q r))

    :action.type/capture-base
    (let [{:keys [action/q action/r]} action]
      (capture-tx db game q r))

    :action.type/end-turn
    (end-turn-tx db game)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; AI Helpers

(defn buildable-unit-types [db game]
  (qess '[:find ?ut
          :in $ ?g
          :where
          [?g  :game/current-faction ?f]
          [?f  :faction/credits ?credits]
          [?ut :unit-type/cost ?cost]
          [(>= ?credits ?cost)]]
        db (e game)))

(defn base-can-act? [db game base]
  (let [{:keys [terrain/q terrain/r]} base]
    (and (> (count (buildable-unit-types db game)) 0)
         (unoccupied? db game q r))))

(defn base-actions [db game base]
  (let [{:keys [terrain/q terrain/r]} base]
    (map (fn [ut]
           {:action/type :action.type/build-unit
            :action/q q
            :action/r r
            :action/unit-type-id (:unit-type/id ut)})
         (buildable-unit-types db game))))

(defn actionable-bases [db game]
  (let [faction (:game/current-faction game)
        bases (faction-bases db faction)]
    (filter #(base-can-act? db game %) bases)))

(defn enemies-in-range [db game unit]
  (let [u-faction (unit-faction db unit)]
    (into []
          (filter #(in-range? db unit %))
          (qess '[:find ?u
                  :in $ ?g ?f-arg
                  :where
                  [?g :game/factions ?f]
                  [?f :faction/units ?u]
                  [(not= ?f ?f-arg)]]
                db (e game) (e u-faction)))))

(defn unit-can-act? [db game unit]
  (let [terrain (terrain-at db game (:unit/q unit) (:unit/r unit))]
    (or (can-move? db game unit)
        (and (can-attack? db game unit)
             (> (count (enemies-in-range db game unit)) 0))
        (can-repair? db game unit)
        (can-capture? db game unit terrain))))

(defn move-actions [db game unit]
  (if (can-move? db game unit)
    (map (fn [move]
           (let [[to-q to-r] (:to move)
                 [from-q from-r] (:from move)]
             (-> move
                 (dissoc :to :from)
                 (assoc :action/type :action.type/move-unit
                        :action/to-q to-q
                        :action/to-r to-r
                        :action/from-q from-q
                        :action/from-r from-r))))
         (valid-moves db game unit))
    []))

(defn attack-actions [db game unit]
  (if (can-attack? db game unit)
    (map (fn [defender]
           {:action/type :action.type/attack-unit
            :action/attacker-q (:unit/q unit)
            :action/attacker-r (:unit/r unit)
            :action/defender-q (:unit/q defender)
            :action/defender-r (:unit/r defender)})
         (enemies-in-range db game unit))
    []))

(defn repair-actions [db game unit]
  (if (can-repair? db game unit)
    [{:action/type :action.type/repair-unit
      :action/q (:unit/q unit)
      :action/r (:unit/r unit)}]
    []))

(defn capture-actions [db game unit]
  (let [{:keys [unit/q unit/r]} unit
        terrain (terrain-at db game q r)]
    (if (can-capture? db game unit terrain)
      [{:action/type :action.type/capture-base
        :action/q q
        :action/r r}]
      [])))

(defn unit-actions [db game unit]
  (concat (move-actions db game unit)
          (attack-actions db game unit)
          (repair-actions db game unit)
          (capture-actions db game unit)))

(defn actionable-units [db game]
  (let [units (get-in game [:game/current-faction :faction/units])]
    (filter #(unit-can-act? db game %) units)))

(defn actionable-actors [db game]
  (concat (actionable-units db game)
          (actionable-bases db game)))

(defn capturable-bases [db game unit]
  (when (get-in unit [:unit/type :unit-type/can-capture])
    (let [u-faction (unit-faction db unit)]
      (into []
            (filter #(not= u-faction (:terrain/owner %)))
            (qess '[:find ?t
                    :in $ ?g
                    :where
                    [?g  :game/map ?m]
                    [?m  :map/terrains ?t]
                    [?t  :terrain/type ?tt]
                    [?tt :terrain-type/id :terrain-type.id/base]]
                  db (e game))))))

(defn closest-capturable-base [db game unit]
  (let [unit-q (:unit/q unit)
        unit-r (:unit/r unit)
        bases (capturable-bases db game unit)]
    (reduce
     (fn [closest terrain]
       (let [terrain-q (:terrain/q terrain)
             terrain-r (:terrain/r terrain)
             closest-q (:terrain/q closest)
             closest-r (:terrain/r closest)]
         (if (< (hex/distance unit-q unit-r terrain-q terrain-r)
                (hex/distance unit-q unit-r closest-q closest-r))
           terrain
           closest)))
     bases)))

;; TODO: return nil if no move is found
(defn closest-move-to-hex [db game unit q r]
  (reduce
   (fn [closest move]
     (if closest
       (let [[closest-q closest-r] (:to closest)
             [move-q move-r] (:to move)]
         (if (< (hex/distance move-q move-r q r)
                (hex/distance closest-q closest-r q r))
           move
           closest))
       move))
   (valid-moves db game unit)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Setup

(defn terrains-spec-tx [terrains-spec]
  (into []
        (map-indexed (fn [i [id terrain-type-spec]]
                       (let [{:keys [image]} terrain-type-spec]
                         {:db/id (- -100 i)
                          :terrain-type/id (keyword 'terrain-type.id (name id))
                          :terrain-type/name (:name terrain-type-spec)
                          :terrain-type/image image})))
        terrains-spec))

(defn attack-strengths-tx [db unit-type-eid attack-strengths-spec]
  (into []
        (map-indexed (fn [i [id attack-strength]]
                       {:db/id (- -201 i)
                        :unit-strength/unit-type unit-type-eid
                        :unit-strength/armor-type (keyword 'unit-type.armor-type (name id))
                        :unit-strength/attack attack-strength}))
        attack-strengths-spec))

(defn terrain-effects-tx [db unit-type-eid terrain-effects-spec]
  (into []
        (map-indexed
         (fn [i [id terrain-effects-spec]]
           (let [{:keys [attack-bonus
                         armor-bonus
                         movement-cost]} terrain-effects-spec
                 terrain-type (find-by db
                                       :terrain-type/id
                                       (keyword 'terrain-type.id (name id)))]
             {:db/id (- -301 i)
              :terrain-effect/terrain-type (e terrain-type)
              :terrain-effect/unit-type unit-type-eid
              :terrain-effect/movement-cost movement-cost
              :terrain-effect/attack-bonus attack-bonus
              :terrain-effect/armor-bonus armor-bonus})))
        terrain-effects-spec))

(defn units-spec-tx [db units-spec]
  (into []
        (comp
         (map-indexed
          (fn [i [id unit-type-spec]]
            (let [{:keys [cost movement can-capture min-range
                          max-range armor capturing-armor repair
                          capturing-armor repair state-map image
                          terrain-effects attack-strengths]} unit-type-spec
                  armor-type (keyword 'unit-type.armor-type
                                      (-> unit-type-spec :armor-type name))
                  unit-state-map-id (keyword 'unit-state-map.id (name state-map))
                  unit-type-eid (- -101 i)]
              (-> [{:db/id unit-type-eid
                    :unit-type/id (keyword 'unit-type.id (name id))
                    :unit-type/name (:name unit-type-spec)
                    :unit-type/cost cost
                    :unit-type/can-capture can-capture
                    :unit-type/movement movement
                    :unit-type/min-range min-range
                    :unit-type/max-range max-range
                    :unit-type/armor-type armor-type
                    :unit-type/armor armor
                    :unit-type/capturing-armor armor
                    :unit-type/repair repair
                    :unit-type/state-map [:unit-state-map/id unit-state-map-id]
                    :unit-type/image image}]
                  (into (attack-strengths-tx db unit-type-eid attack-strengths))
                  (into (terrain-effects-tx db unit-type-eid terrain-effects))))))
         cat)
        units-spec))

(defn unit-state-map-id [state-map-name]
  (keyword 'unit-state-map.id (name state-map-name)))

(defn unit-state-id [state-map-name state-name]
  (keyword 'unit-state.id (str (name state-map-name) "--" (name state-name))))

(defn action-type [action-type-name]
  (keyword 'action.type (name action-type-name)))

(defn unit-state-transitions-tx [state-map-name parent-state-name transitions]
  (into []
        (map-indexed
         (fn [i [action new-state]]
           (let [parent-state-id (unit-state-id state-map-name parent-state-name)
                 new-state-id (unit-state-id state-map-name new-state)]
             {:db/id (- -301 i)
              :unit-state-transition/action-type (action-type action)
              :unit-state-transition/new-state [:unit-state/id new-state-id]
              :unit-state/_transitions [:unit-state/id parent-state-id]})))
        transitions))

(defn unit-states-tx [state-map-name states]
  (into []
        (comp
         (map-indexed
          (fn [i [state-name transitions]]
            (let [parent-map-id (unit-state-map-id state-map-name)
                  state-id (unit-state-id state-map-name state-name)]
              (into [{:db/id (- -201 i)
                      :unit-state/id state-id
                      :unit-state-map/_states [:unit-state-map/id parent-map-id]}]
                    (unit-state-transitions-tx state-map-name state-name
                                               transitions)))))
         cat)
        states))

(defn unit-state-map-tx [state-maps]
  (into []
        (comp
         (map-indexed
          (fn [i [state-map-name state-map]]
            (let [{:keys [states start-turn-state newly-built-state]} state-map
                  map-id (unit-state-map-id state-map-name)
                  start-id (unit-state-id state-map-name start-turn-state)
                  built-id (unit-state-id state-map-name newly-built-state)]
              (-> [{:db/id (- -101 i)
                    :unit-state-map/id map-id}]
                  (into (unit-states-tx state-map-name states))
                  (into [{:db/id (- -101 i)
                          :unit-state-map/start-turn-state [:unit-state/id start-id]
                          :unit-state-map/newly-built-state [:unit-state/id built-id]}])))))
         cat)
        state-maps))

(defn load-specs! [conn]
  (d/transact! conn (terrains-spec-tx data/terrains))
  (d/transact! conn (unit-state-map-tx data/unit-state-maps))
  (d/transact! conn (units-spec-tx @conn data/units)))

(defn game-map-tx [game map-def]
  (let [map-eid -101]
    (into [{:db/id map-eid
            :map/id (:id map-def)
            :map/name (:name map-def)
            :game/_map (e game)}]
          (map-indexed
           (fn [i t]
             (let [{:keys [q r]} t]
               {:db/id (- -201 i)
                :terrain/game-pos-idx (game-pos-idx game q r)
                :terrain/q q
                :terrain/r r
                :terrain/type [:terrain-type/id (->> (:terrain-type t)
                                                     name
                                                     (keyword 'terrain-type.id))]
                :map/_terrains map-eid}))
           (:terrains map-def)))))

(defn create-game!
  ([conn scenario-def]
   (create-game! conn scenario-def {}))
  ([conn scenario-def game-state]
   (let [game-id (random-uuid)
         {:keys [id credits-per-base max-unit-count]} scenario-def]
     (d/transact! conn [{:db/id -101
                         :game/id game-id
                         :game/scenario-id id
                         :game/round (get game-state :round 1)
                         :game/max-unit-count max-unit-count
                         :game/credits-per-base credits-per-base}])
     game-id)))

(defn bases-tx [game scenario-def]
  (for [base (:bases scenario-def)]
    (let [{:keys [q r]} base]
      {:terrain/game-pos-idx (game-pos-idx game q r)
       :terrain/q q
       :terrain/r r
       :terrain/type [:terrain-type/id :terrain-type.id/base]
       :map/_terrains (e (:game/map game))})))

(defn factions-tx [game factions]
  (map-indexed (fn [i faction]
                 (let [{:keys [color credits ai]} faction
                       next-id (- -101 (mod (inc i) (count factions)))]
                   {:db/id (- -101 i)
                    :faction/color (to-faction-color color)
                    :faction/credits credits
                    :faction/ai ai
                    :faction/order (inc i)
                    :faction/next-faction next-id
                    :game/_factions (e game)}))
               factions))

(defn factions-bases-tx [db game factions]
  (mapcat (fn [faction]
            (let [{:keys [bases color]} faction
                  faction-eid (e (faction-by-color db game color))]
              (map (fn [{:keys [q r] :as base}]
                     (let [idx (game-pos-idx game q r)]
                       {:terrain/game-pos-idx idx
                        :terrain/owner faction-eid}))
                   bases)))
          factions))

(defn factions-units-tx [db game factions]
  (mapcat (fn [[i faction]]
            (let [{:keys [game/max-unit-count]} game
                  {:keys [units color]} faction
                  faction-eid (e (faction-by-color db game color))]
              (map-indexed
                (fn [j {:keys [q r] :as unit}]
                  (let [idx (game-pos-idx game q r)
                        unit-type-id (to-unit-type-id (:unit-type unit))
                        capturing (get unit :capturing false)]
                    (cond-> {:db/id (- (- (* i 100)) (inc j))
                             :unit/game-pos-idx idx
                             :unit/q q
                             :unit/r r
                             :unit/count (get unit :count max-unit-count)
                             :unit/round-built (get unit :round-built 0)
                             :unit/move-count (get unit :move-count 0)
                             :unit/attack-count (get unit :attack-count 0)
                             :unit/attacked-count (get unit :attack-count 0)
                             :unit/repaired (get unit :repaired false)
                             :unit/capturing capturing
                             :unit/type [:unit-type/id unit-type-id]
                             :faction/_units faction-eid}
                      capturing
                      (assoc :unit/capture-round (:capture-round unit)))))
                units)))
          (zipmap (range) factions)))

;; TODO: set unit/state
(defn load-scenario! [conn map-defs scenario-def]
  (let [game-id (create-game! conn scenario-def)
        conn-game #(game-by-id @conn game-id)
        {:keys [map-id credits-per-base factions]} scenario-def
        starting-faction-color (get-in factions [0 :color])]
    (d/transact! conn (game-map-tx (conn-game) (map-defs map-id)))
    (d/transact! conn (bases-tx (conn-game) scenario-def))
    (d/transact! conn (factions-tx (conn-game) factions))
    (d/transact! conn (factions-bases-tx @conn (conn-game) factions))
    (d/transact! conn (factions-units-tx @conn (conn-game) factions))
    (let [game (conn-game)
          starting-faction-eid (->> starting-faction-color
                                    (faction-by-color @conn game)
                                    e)]
      (d/transact! conn [{:db/id (e game)
                          :game/starting-faction starting-faction-eid
                          :game/current-faction starting-faction-eid}]))
    game-id))

;; Info needed to load game from URL
;; - scenario id
;; - current-faction
;; - faction
;;   - credits
;;   - ai status
;;   - owned bases
;;     - location
;;   - units
;;     - location
;;     - health
;;     - round-built
;;     - capturing

(defn load-game-state! [conn map-defs scenario-defs game-state]
  (let [{:keys [scenario-id current-faction factions]} game-state
        scenario-def (scenario-defs scenario-id)
        starting-faction (get-in scenario-def [:factions 0 :color])
        game-id (create-game! conn scenario-def game-state)
        conn-game #(game-by-id @conn game-id)
        {:keys [map-id credits-per-base]} scenario-def]
    (d/transact! conn (game-map-tx (conn-game) (map-defs map-id)))
    (d/transact! conn (bases-tx (conn-game) scenario-def))
    (d/transact! conn (factions-tx (conn-game) factions))
    (d/transact! conn (factions-bases-tx @conn (conn-game) factions))
    (d/transact! conn (factions-units-tx @conn (conn-game) factions))
    (let [game (conn-game)
          starting-faction-eid (e (faction-by-color @conn game starting-faction))
          current-faction-eid (e (faction-by-color @conn game current-faction))]
      (d/transact! conn [{:db/id (-> (conn-game) :game/map e)
                          :map/starting-faction starting-faction-eid}
                         {:db/id (e game)
                          :game/current-faction current-faction-eid}]))
    game-id))

;; TODO: figure out better name
(defn get-game-state
  ([db game]
   (get-game-state db game :minimal))
  ([db game dump-type]
   (let [factions (->> game :game/factions (sort-by :order))]
     {:scenario-id (:game/scenario-id game)
      :round (:game/round game)
      :current-faction (-> game
                           :game/current-faction
                           :faction/color
                           name
                           keyword)
      :factions
      (into []
            (for [faction factions]
              {:credits (:faction/credits faction)
               :ai (:faction/ai faction)
               :color (-> (:faction/color faction)
                          name
                          keyword)
               :bases
               (into []
                     (for [base (faction-bases db faction)]
                       {:q (:terrain/q base)
                        :r (:terrain/r base)}))
               :units
               (into []
                     (for [unit (:faction/units faction)]
                       (cond-> {:q (:unit/q unit)
                                :r (:unit/r unit)
                                :unit-type (-> unit
                                               :unit/type
                                               :unit-type/id
                                               name
                                               keyword)
                                :count (:unit/count unit)}
                         (:unit/capturing unit)
                         (assoc :capturing true
                                :capture-round (:unit/capture-round unit))

                         (= dump-type :full)
                         (assoc :attack-count (:unit/attack-count unit)
                                :move-count (:unit/move-count unit)
                                :repaired (:unit/repaired unit)
                                :round-built (:unit/round-built unit))
                         )))
               }))
      })))
