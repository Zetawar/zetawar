(ns zetawar.game
  (:require
   [clojure.string :as string]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.data :as data]
   [zetawar.db :as db :refer [e find-by qe qes qess]]
   [zetawar.hex :as hex]
   [zetawar.util :refer [breakpoint inspect oonly only]]))

;; TODO: improve exception data

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Util

;; TODO: benchmark vs bit shifting and with/without memoization
(def game-pos-idx
  (memoize
   (fn game-pos-idx [game q r]
     (if (and game q r)
       (+ r (* 1000 (+ (* (e game) 1000) q)))
       -1))))

(defn game-id-idx [game-or-game-id id]
  (let [game-id (if (:db/id game-or-game-id)
                  (:game/id game-or-game-id)
                  game-or-game-id)]
    (-> id
        str
        (string/split #":")
        (nth 1)
        (str "-" game-id)
        keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game

(defn game-by-id [db game-id]
  (find-by db :game/id game-id))

(defn current-faction-color [game]
  (get-in game [:game/current-faction :faction/color]))

(defn next-faction-color [game]
  (get-in game [:game/current-faction :faction/next-faction :faction/color]))

(defn game-ex [message game]
  (ex-info message (select-keys game [:game/self-repair])))

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

(defn faction-count [db game]
  (-> (d/q '[:find (count ?f)
             :in $ ?g
             :where
             [?g :game/factions ?f]]
           db (e game))
      ffirst
      (or 0)))

(defn ai-faction-count [db game]
  (-> (d/q '[:find (count ?f)
             :in $ ?g
             :where
             [?g :game/factions ?f]
             [?f :faction/ai true]]
           db (e game))
      ffirst
      (or 0)))

(defn human-faction-count [db game]
  (- (faction-count db game)
     (ai-faction-count db game)))

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

(defn faction-base-being-captured-count [db faction]
  (-> (d/q '[:find (count ?b)
             :in $ ?f
             :where
             [?b :terrain/owner ?f]
             [(not= ?ef f)]
             [?ef :faction/units ?u]
             [?u :unit/terrain ?b]
             [?u :unit/capturing true]]
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
        captured-count (faction-base-being-captured-count db faction)
        {:keys [game/credits-per-base]} game]
    (* (- base-count captured-count) credits-per-base)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Terrain

(defn to-terrain-type-id [terrain-type-name]
  (->> terrain-type-name
       name
       (keyword 'terrain-type.id)))

(defn terrain-type-by-id [db game terrain-type-id]
  (qe '[:find ?tt
        :in $ ?g ?tt-id
        :where
        [?g  :game/terrain-types ?tt]
        [?tt :terrain-type/id ?tt-id]]
      db (e game) terrain-type-id))

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
  (let [terrain (terrain-at db game q r)]
    (when (base? terrain)
      terrain)))

(defn checked-base-at [db game q r]
  (let [base (base-at db game q r)]
    (when-not base
      (throw (ex-info "No base at specified coordinates"
                      {:q q :r r})))
    base))

(defn check-base-current [db game base]
  (let [cur-faction (:game/current-faction game)
        base-faction (:terrain/owner base)]
    (when (not= cur-faction base-faction)
      (throw (ex-info "Base is not owned by the current faction"
                      {:current-faction (:faction/color cur-faction)
                       })))))

(defn current-base? [db game x]
  (when (base? x)
    (let [cur-faction (:game/current-faction game)
          base-faction (:terrain/owner x)]
      (= cur-faction base-faction))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Units

(defn to-unit-type-id [unit-type-name]
  (->> unit-type-name
       name
       (keyword 'unit-type.id)))

(defn to-armor-type [armor-type-name]
  (->> armor-type-name
       name
       (keyword 'unit-type.armor-type)))

(defn unit? [x]
  (contains? x :unit/type))

(defn unit-hex [unit]
  [(:unit/q unit)
   (:unit/r unit)])

(defn unit-color [unit]
  (get-in unit [:faction/_units :faction/color]))

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

(defn unit-faction [db unit]
  (:faction/_units unit))

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

(defn on-owned-base? [db game unit]
  (let [{:keys [unit/q unit/r]} unit
        terrain (terrain-at db game q r)]
    (and (base? terrain)
         (= (some-> terrain :terrain/owner e)
            (e (unit-faction db unit))))))

(defn on-capturable-base? [db game unit]
  (let [{:keys [unit/q unit/r]} unit
        terrain (terrain-at db game q r)]
    (and (base? terrain)
         (not= (some-> terrain :terrain/owner e)
               (e (unit-faction db unit))))))

(defn unit-ex [message unit]
  (ex-info message (select-keys unit [:unit/q :unit/r])))

(defn unit-terrain-effects [db unit terrain]
  (only (d/q '[:find ?mc ?at ?ar
               :in $ ?u ?t
               :where
               [?u  :unit/type ?ut]
               [?t  :terrain/type ?tt]
               [?tt :terrain-type/effects ?e]
               [?e  :terrain-effect/unit-type ?ut]
               [?e  :terrain-effect/attack-bonus ?at]
               [?e  :terrain-effect/armor-bonus ?ar]
               [?e  :terrain-effect/movement-cost ?mc]]
             db (e unit) (e terrain))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Unit States

(defn to-unit-state-map-id [state-map-name]
  (->> state-map-name
       name
       (keyword 'unit-state-map.id)))

(defn unit-state-map-by-id [db game unit-state-map-id]
  (qe '[:find ?usm
        :in $ ?g ?usm-id
        :where
        [?g   :game/unit-state-maps ?usm]
        [?usm :unit-state-map/id ?usm-id]]
      db (e game) unit-state-map-id))

(defn to-unit-state-id
  ([unit-state-name]
   (->> unit-state-name
        name
        (keyword 'unit-state.id)))
  ([state-map-name state-name]
   (to-unit-state-id (str (name state-map-name)
                          "_"
                          (name state-name)))))

(defn unit-state-by-id [db game unit-state-id]
  (qe '[:find ?us
        :in $ ?g ?us-id
        :where
        [?g  :game/unit-states ?us]
        [?us :unit-state/id ?us-id]]
      db (e game) unit-state-id))

(defn to-action-type [action-type-name]
  (->> action-type-name name (keyword 'action.type)))

(defn start-state [unit-or-unit-type]
  (let [unit-type (if (unit? unit-or-unit-type)
                    (:unit/type unit-or-unit-type)
                    unit-or-unit-type)]
    (get-in unit-type [:unit-type/state-map
                       :unit-state-map/start-state])))

(defn built-state [unit-or-unit-type]
  (let [unit-type (if (unit? unit-or-unit-type)
                    (:unit/type unit-or-unit-type)
                    unit-or-unit-type)]
    (get-in unit-type [:unit-type/state-map
                       :unit-state-map/built-state])))

;; Note: (->> (e unit) (d/entity db) ...) is to support UI subs
(defn next-state [db unit action]
  (let [transition-map (->> (e unit)
                            (d/entity db)
                            :unit/state
                            :unit-state/transitions
                            (map (juxt :unit-state-transition/action-type
                                       :unit-state-transition/new-state))
                            (into {}))]
    (transition-map action)))

(defn checked-next-state [db unit action]
  (let [new-state (next-state db unit action)]
    (when-not new-state
      (throw (ex-info "No state transition from current state found for action"
                      {:current-state (get-in unit [:unit/state :unit-state/id])
                       :action action})))
    new-state))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Movement

(defn valid-moves [db game unit]
  (let [start [(:unit/q unit) (:unit/r unit)]
        u-faction-eid (e (unit-faction db unit))
        unit-type-eid (e (:unit/type unit))
        armor-type (-> unit :unit/type :unit-type/armor-type)
        unit-movement (get-in unit [:unit/type :unit-type/movement])
        unit-at (memoize #(unit-at db game %1 %2))
        terrain-type->cost (into {} (d/q '[:find ?tt ?mc
                                           :in $ ?ut
                                           :where
                                           [?tt :terrain-type/effects ?e]
                                           [?e  :terrain-effect/unit-type ?ut]
                                           [?e  :terrain-effect/movement-cost ?mc]]
                                         db unit-type-eid))
        terrain-cost-at (memoize (fn terrain-cost-at [q r]
                                   (some-> (terrain-at db game q r)
                                           :terrain/type
                                           e
                                           terrain-type->cost)))
        adjacent-costs (memoize (fn adjacent-costs [q r]
                                  (into []
                                        (keep #(when-let [cost (apply terrain-cost-at %)]
                                                 (conj % cost)))
                                        (hex/adjacents q r))))
        enemy-at? (memoize (fn enemy-at? [q r]
                             (some-> (unit-at q r)
                                     :faction/_units
                                     e
                                     (not= u-faction-eid))))
        zoc-enemy-at? (memoize (fn zoc-enemy-at? [q r]
                                 (when-let [other-unit (unit-at q r)]
                                   (let [ou-faction-eid (-> other-unit :faction/_units e)
                                         zoc-armor-types (-> other-unit :unit/type :unit-type/zoc-armor-types)]
                                     (and (not= u-faction-eid ou-faction-eid)
                                          (contains? zoc-armor-types armor-type))))))
        adjacent-zoc-enemy? (memoize (fn adjacent-enemy? [q r]
                                       (some #(apply zoc-enemy-at? %) (hex/adjacents q r))))
        ;; frontier = {[q r] [cost path], ...}
        ;; moves = {[q r] [cost path], ...}
        expand-frontier (fn expand-frontier [frontier moves]
                          (loop [[[[q r] [frontier-cost path]] & remaining-frontier] frontier new-frontier {}]
                            (if frontier-cost
                              (let [remaining-movement (- unit-movement frontier-cost)
                                    terrain-costs (adjacent-costs q r)
                                    new-moves (into {}
                                                    (keep (fn [[q r terrain-cost]]
                                                            (when (and (or (:game/move-through-friendly game)
                                                                           (not (unit-at q r)))
                                                                       (not (enemy-at? q r)))
                                                              (let [terrain-cost (if (adjacent-zoc-enemy? q r) ; check in zoc
                                                                                   (max terrain-cost remaining-movement)
                                                                                   terrain-cost)
                                                                    new-move-cost (+ frontier-cost terrain-cost)]
                                                                (when (and (<= new-move-cost (moves [q r] unit-movement))
                                                                           (<= new-move-cost (new-frontier [q r] unit-movement)))
                                                                  [[q r] [new-move-cost (conj path [q r])]])))))
                                                    terrain-costs)]
                                (recur remaining-frontier (conj new-frontier new-moves)))
                              new-frontier)))]
    (loop [frontier {start [0 []]} moves {start [0 []]}]
      (let [new-frontier (expand-frontier frontier moves)
            new-moves (conj moves new-frontier)]
        (if (empty? new-frontier)
          (into #{}
                (comp (remove #(apply unit-at (first %))) ; remove occupied destinations
                      (map (fn [[dest [cost path]]] {:from start :to dest :cost cost :path path})))
                (dissoc moves start))
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
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot move while capturing" unit)))
  (checked-next-state db unit :action.type/move-unit))

(defn can-move? [db game unit]
  (try
    (check-can-move db game unit)
    true
    (catch :default ex
      false)))

(defn teleport-tx [db game from-q from-r to-q to-r]
  (let [unit (checked-unit-at db game from-q from-r)
        terrain (checked-terrain-at db game to-q to-r)]
    [{:db/id (e unit)
      :unit/game-pos-idx (game-pos-idx game to-q to-r)
      :unit/q to-q
      :unit/r to-r
      :unit/terrain (e terrain)}]))

;; TODO: check move is valid
(defn move-tx
  "Returns a transaction that updates the unit's location and move count."
  ([db game unit to-terrain]
   (let [new-move-count (inc (:unit/move-count unit 0))
         [to-q to-r] (terrain-hex to-terrain)
         new-state (check-can-move db game unit)]
     [{:db/id (e unit)
       :unit/game-pos-idx (game-pos-idx game to-q to-r)
       :unit/q to-q
       :unit/r to-r
       :unit/terrain (e to-terrain)
       :unit/move-count new-move-count
       :unit/state (e new-state)}]))
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
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot attack while capturing" unit)))
  (checked-next-state db unit :action.type/attack-unit))

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

(defn attack-damage [db game attacker defender attacker-terrain defender-terrain]
  (let [defender-armor-type (get-in defender [:unit/type :unit-type/armor-type])
        [attacker-q attacker-r] (unit-hex attacker)
        [defender-q defender-r] (unit-hex defender)
        attack-strength (oonly (d/q '[:find ?s
                                      :in $ ?u ?at
                                      :where
                                      [?u  :unit/type ?ut]
                                      [?ut :unit-type/strengths ?us]
                                      [?us :unit-strength/armor-type ?at]
                                      [?us :unit-strength/attack ?s]]
                                    db (e attacker) defender-armor-type))
        armor (if (:unit/capturing defender)
                (get-in defender [:unit/type :unit-type/capturing-armor])
                (get-in defender [:unit/type :unit-type/armor]))
        attack-bonus (oonly (d/q '[:find ?a
                                   :in $ ?u ?t
                                   :where
                                   [?u  :unit/type ?ut]
                                   [?t  :terrain/type ?tt]
                                   [?tt :terrain-type/effects ?e]
                                   [?e  :terrain-effect/unit-type ?ut]
                                   [?e  :terrain-effect/attack-bonus ?a]]
                                 db (e attacker) (e attacker-terrain)))
        armor-bonus (oonly (d/q '[:find ?d
                                  :in $ ?u ?t
                                  :where
                                  [?u  :unit/type ?ut]
                                  [?t  :terrain/type ?tt]
                                  [?tt :terrain-type/effects ?e ]
                                  [?e  :terrain-effect/unit-type ?ut]
                                  [?e  :terrain-effect/armor-bonus ?d]]
                                db (e defender) (e defender-terrain)))
        attack-hexes (into #{} (map terrain-hex) (:unit/attacked-from defender))
        ranged-attack-hexes (into #{}
                                  (filter #(> (apply hex/distance defender-q defender-r %) 1))
                                  attack-hexes)
        adjacent-attack-hexes (into #{}
                                    (filter #(and (apply hex/adjacent? attacker-q attacker-r %)
                                                  (apply hex/adjacent? defender-q defender-r %)))
                                    attack-hexes)
        opposite-attack-hexes (into #{}
                                    (filter #(apply hex/opposite? attacker-q attacker-r defender-q defender-r %))
                                    attack-hexes)
        flanking-attack-hexes (clojure.set/difference attack-hexes
                                                      ranged-attack-hexes
                                                      adjacent-attack-hexes
                                                      opposite-attack-hexes)
        gang-up-bonus (+ (* (count ranged-attack-hexes)
                            (:game/ranged-attack-bonus game))
                         (* (count adjacent-attack-hexes)
                            (:game/adjacent-attack-bonus game))
                         (* (count flanking-attack-hexes)
                            (:game/flanking-attack-bonus game))
                         (* (count opposite-attack-hexes)
                            (:game/opposite-attack-bonus game)))]
    (let [p (-> (+ 0.5 (* 0.05 (+ (- (+ attack-strength attack-bonus)
                                     (+ armor armor-bonus))
                                  gang-up-bonus)))
                (max 0)
                (min 1))]
      (js/Math.round
       (if (:game/stochastic-damage game)
         (let [hits (->> (repeatedly #(rand))
                         (take (* 6 (:unit/count attacker)))
                         (filter #(< % p))
                         count)]
           (quot hits 6))
         (* (:unit/count attacker) p))))))

(defn battle-damage
  ([db game attacker defender]
   (let [attacker-terrain (:unit/terrain attacker)
         defender-terrain (:unit/terrain defender)
         attack-count (:unit/attack-count attacker 0)
         defender-damage (attack-damage db game attacker defender attacker-terrain defender-terrain)
         attacker-damage (if (in-range? db defender attacker)
                           (attack-damage db game defender attacker defender-terrain attacker-terrain)
                           0)]
     [(min (:unit/count attacker) attacker-damage)
      (min (:unit/count defender) defender-damage)]))
  ([db game attacker-q attacker-r defender-q defender-r]
   (let [attacker (checked-unit-at db game attacker-q attacker-r)
         defender (checked-unit-at db game defender-q defender-r)]
     (battle-damage db game attacker defender))))

(defn battle-tx
  ([db game attacker defender attacker-damage defender-damage]
   (let [new-state (check-can-attack db game attacker)]
     (check-in-range db attacker defender)
     (let [attacker-terrain (:unit/terrain attacker)
           defender-terrain (:unit/terrain defender)
           attack-count (:unit/attack-count attacker 0)
           attacker-count (:unit/count attacker)
           defender-count (:unit/count defender)]
       (cond-> []
         (> defender-count defender-damage)
         (conj {:db/id (e defender)
                :unit/count (- defender-count defender-damage)
                :unit/attacked-count (inc (:unit/attacked-count defender))
                :unit/attacked-from (e attacker-terrain)})

         (= defender-count defender-damage)
         (conj [:db.fn/retractEntity (e defender)])

         (> attacker-count attacker-damage)
         (conj {:db/id (e attacker)
                :unit/count (- attacker-count attacker-damage)
                :unit/attack-count (inc attack-count)
                :unit/state (e new-state)})

         (= attacker-count attacker-damage)
         (conj [:db.fn/retractEntity (e attacker)])))))
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

(defn check-repairable [db game unit]
  (when (>= (:unit/count unit) (:game/max-count-per-unit game))
    (throw (unit-ex "Unit is already fully repaired" unit)))
  unit)

(defn repairable? [db game unit]
  (try
    (check-repairable db game unit)
    true
    (catch :default ex
      false)))

(defn check-can-repair [db game unit]
  (check-unit-current db game unit)
  (when-not (or (:game/self-repair game) (on-owned-base? db game unit))
    (throw (game-ex "Unit self repair is not allowed" game)))
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot make repairs while capturing" unit)))
  (check-repairable db game unit)
  (checked-next-state db unit :action.type/repair-unit))

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
   (let [new-state (check-can-repair db game unit)]
     [{:db/id (e unit)
       :unit/count (min (:game/max-count-per-unit game)
                        (+ (:unit/count unit)
                           (get-in unit [:unit/type :unit-type/repair])))
       :unit/repaired true
       :unit/state (e new-state)}]))
  ([db game q r]
   (let [unit (checked-unit-at db game q r)]
     (repair-tx db game unit))))

(defn repair! [conn game-id q r]
  (let [db @conn
        game (game-by-id db game-id)
        tx (repair-tx db game q r)]
    (d/transact! conn tx)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Field Repair

(defn check-can-field-repair [db game unit]
  (check-unit-current db game unit)
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit cannot make repairs while capturing" unit)))
  (when (empty? (get-in unit [:unit/type :unit-type/can-repair]))
    (throw (unit-ex "Unit cannot repair other units" unit)))
  (checked-next-state db unit :action.type/field-repair-unit))

(defn can-field-repair? [db game unit]
  (try
    (check-can-field-repair db game unit)
    true
    (catch :default ex
      false)))

(defn check-has-repairable-armor-type [db game repairer target]
  (let [possible-repair-types (get-in repairer [:unit/type :unit-type/can-repair])
        goal-repair-type (get-in target [:unit/type :unit-type/armor-type])]
    (when-not (some #{goal-repair-type} possible-repair-types)
      (throw (unit-ex "Armor types are not compatible" repairer)))
    repairer))

(defn has-repairable-armor-type? [db game repairer target]
  (try
    (check-has-repairable-armor-type db game repairer target)
    true
    (catch :default ex
      false)))

(defn field-repair-tx
  ([db game repairer target]
   (let [new-state (check-can-field-repair db game repairer)]
     (check-in-range db repairer target)
     (check-repairable db game target)
     (check-has-repairable-armor-type db game repairer target)
     [{:db/id (e target)
       :unit/count (min (:game/max-count-per-unit game)
                        (+ (:unit/count target)
                           (get-in repairer [:unit/type :unit-type/repair])))}
      {:db/id (e repairer)
       :unit/state (e new-state)}]))
  ([db game q1 r1 q2 r2]
   (let [repairer (checked-unit-at db game q1 r1)
         target (checked-unit-at db game q2 r2)]
     (field-repair-tx db game repairer target))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Capture

;; TODO: add can-capture? that only checks if unit type can capture

;; TODO: rename to check-can-capture
(defn check-capturable [db game unit terrain]
  (check-unit-current db game unit)
  (when-not (-> unit :unit/type :unit-type/can-capture)
    (throw (unit-ex "Unit does not have the ability to capture" unit)))
  (when-not (and terrain (base? terrain))
    (throw (unit-ex "Unit unit is not on a base" unit)))
  (when (:unit/capturing unit)
    (throw (unit-ex "Unit is already caturing" unit)))
  (when (= (e (unit-faction db unit)) (some-> terrain :terrain/owner e))
    ;; TODO: add more exception info
    (throw (ex-info "Base is already owned by current faction"
                    {:q (:terrain/q terrain)
                     :r (:terrain/r terrain)})))
  (checked-next-state db unit :action.type/capture-base))

;; TODO: rename to can-capture-terrain? (?)
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
         round (:game/round game)
         new-state (check-capturable db game unit base)]
     [{:db/id (e unit)
       :unit/capturing true
       :unit/capture-round (inc round)
       :unit/state (e new-state)}]))
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
       :unit/terrain (e base)
       :unit/round-built (:game/round game)
       :unit/type (e unit-type)
       :unit/count (:game/max-count-per-unit game)
       :unit/move-count 0
       :unit/attack-count 0
       :unit/attacked-count 0
       :unit/repaired false
       :unit/capturing false
       :unit/state (-> unit-type built-state e)}
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

(defn unit-end-turn-tx [db game unit]
  (let [{:keys [unit/q unit/r unit/capturing unit/capture-round]} unit]
    (if (and capturing (= capture-round (:game/round game)))
      (let [faction (unit-faction db unit)
            terrain (checked-base-at db game q r)]
        [[:db/add (e terrain) :terrain/owner (e faction)]
         [:db.fn/retractEntity (e unit)]])
      (into [{:db/id (e unit)
              :unit/repaired false
              :unit/move-count 0
              :unit/attack-count 0
              :unit/attacked-count 0
              :unit/state (-> unit start-state e)}]
            (map (fn [u] [:db/retract (e unit) :unit/attacked-from (e u)]))
            (:unit/attacked-from unit)))))

(defn end-turn-tx
  "Return a transaction that completes captures, clears per round unit flags,
  updates the current faction, adds faction credits, and updates the round."
  [db game]
  (let [starting-faction (:game/starting-faction game)
        cur-faction (:game/current-faction game)
        next-faction (:faction/next-faction cur-faction)
        cur-round (:game/round game)
        new-round (if (= starting-faction next-faction)
                    (inc cur-round)
                    cur-round)
        credits (+ (:faction/credits next-faction)
                   (if (> new-round 1)
                     (income db game next-faction)
                     0))
        units (:faction/units cur-faction)
        attacked-froms (:game/attacked-froms game)]
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

    :action.type/field-repair-unit
    (let [{:keys [action/repairer-q action/repairer-r
                  action/target-q action/target-r]} action]
      (field-repair-tx db game
                       repairer-q repairer-r
                       target-q target-r))

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

;; TODO: rename to enemy-units (?)
(defn enemies [db game unit]
  (let [u-faction (unit-faction db unit)]
    (qess '[:find ?u
            :in $ ?g ?f-arg
            :where
            [?g :game/factions ?f]
            [?f :faction/units ?u]
            [(not= ?f ?f-arg)]]
          db (e game) (e u-faction))))

(defn enemies-in-range [db game unit]
  (into []
        (filter #(in-range? db unit %))
        (enemies db game unit)))

(defn closest-enemy [db game unit]
  (let [unit-q (:unit/q unit)
        unit-r (:unit/r unit)]
    (reduce
     (fn [closest enemy]
       (let [enemy-q (:unit/q enemy)
             enemy-r (:unit/r enemy)
             closest-q (:unit/q closest)
             closest-r (:unit/r closest)]
         (if (< (hex/distance unit-q unit-r enemy-q enemy-r)
                (hex/distance unit-q unit-r closest-q closest-r))
           enemy
           closest)))
     (enemies db game unit))))

(defn friends [db game unit]
  (let [u-faction (unit-faction db unit)]
    (qess '[:find ?u
            :in $ ?g ?f-arg
            :where
            [?g :game/factions ?f]
            [?f :faction/units ?u]
            [(= ?f ?f-arg)]]
          db (e game) (e u-faction))))

(defn friends-in-range [db game unit]
  (into []
        (filter #(in-range? db unit %))
        (friends db game unit)))

(defn repairable-friends-in-range [db game unit]
  (into []
        (filter #(repairable? db game %))
        (friends-in-range db game unit)))

(defn closest-friend [db game unit]
  (let [unit-q (:unit/q unit)
        unit-r (:unit/r unit)]
    (reduce
     (fn [closest friend]
       (let [friend-q (:unit/q friend)
             friend-r (:unit/r friend)
             closest-q (:unit/q closest)
             closest-r (:unit/r closest)]
         (if (< (hex/distance unit-q unit-r friend-q friend-r)
                (hex/distance unit-q unit-r closest-q closest-r))
           friend
           closest)))
     (friends db game unit))))

(defn unit-can-act? [db game unit]
  (let [terrain (:unit/terrain unit)]
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

(defn field-repair-actions [db game unit]
  (if (can-field-repair? db game unit)
    (map (fn [target]
           {:action/type :action.type/field-repair-unit
            :action/repairer-q (:unit/q unit)
            :action/repairer-r (:unit/r unit)
            :action/target-q (:unit/q target)
            :action/target-r (:unit/r target)})
         (friends-in-range db game unit))
    []))

(defn capture-actions [db game unit]
  (let [{:keys [unit/q unit/r unit/terrain]} unit]
    (if (can-capture? db game unit terrain)
      [{:action/type :action.type/capture-base
        :action/q q
        :action/r r}]
      [])))

(defn unit-actions [db game unit]
  (concat (move-actions db game unit)
          (attack-actions db game unit)
          (repair-actions db game unit)
          (field-repair-actions db game unit)
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

(defn settings-tx [db game-id settings-def]
  (let [game (game-by-id db game-id)]
    [{:db/id (e game)
      :game/ranged-attack-bonus (:ranged-attack-bonus settings-def)
      :game/adjacent-attack-bonus (:adjacent-attack-bonus settings-def)
      :game/flanking-attack-bonus (:flanking-attack-bonus settings-def)
      :game/opposite-attack-bonus (:opposite-attack-bonus settings-def)
      :game/stochastic-damage (:stochastic-damage settings-def)
      :game/self-repair (:self-repair settings-def)
      :game/move-through-friendly (:move-through-friendly settings-def)}]))

(defn terrain-types-tx [db game-id terrains-def]
  (let [game (game-by-id db game-id)]
    (into []
          (map
           (fn [[terrain-type-name terrain-def]]
             (let [terrain-type-id (to-terrain-type-id terrain-type-name)
                   terrain-type-idx (game-id-idx game-id terrain-type-id)]
               {:db/id (db/next-temp-id)
                :game/_terrain-types (e game)
                :terrain-type/id terrain-type-id
                :terrain-type/game-id-idx terrain-type-idx
                :terrain-type/description (:description terrain-def)
                :terrain-type/image (:image terrain-def)})))
          terrains-def)))

(defn attack-strengths-tx [db game-id unit-type-eid attack-strengths-def]
  (let [game (game-by-id db game-id)]
    (into []
          (map
           (fn [[armor-type-name attack-strength]]
             {:db/id (db/next-temp-id)
              :unit-type/_strengths unit-type-eid
              :unit-strength/armor-type (to-armor-type armor-type-name)
              :unit-strength/attack attack-strength}))
          attack-strengths-def)))

(defn terrain-effects-tx [db game-id unit-type-eid terrain-effects-def]
  (let [game (game-by-id db game-id)]
    (into []
          (map
           (fn [[terrain-type-name terrain-effect-def]]
             (let [{:keys [attack-bonus armor-bonus movement-cost]} terrain-effect-def
                   terrain-type-idx (->> terrain-type-name to-terrain-type-id (game-id-idx game-id))]
               {:db/id (db/next-temp-id)
                :terrain-type/_effects [:terrain-type/game-id-idx terrain-type-idx]
                :terrain-effect/unit-type unit-type-eid
                :terrain-effect/movement-cost movement-cost
                :terrain-effect/attack-bonus attack-bonus
                :terrain-effect/armor-bonus armor-bonus})))
          terrain-effects-def)))

(defn unit-types-tx [db game-id units-def]
  (let [game (game-by-id db game-id)]
    (into []
          (mapcat
           (fn [[unit-type-name unit-def]]
             (let [{:keys [armor capturing-armor state-map]} unit-def
                   unit-state-map-idx (->> state-map to-unit-state-map-id (game-id-idx game-id))
                   unit-type-eid (db/next-temp-id)]
               (-> [{:db/id unit-type-eid
                     :game/_unit-types (e game)
                     :unit-type/id (to-unit-type-id unit-type-name)
                     :unit-type/description (:description unit-def)
                     :unit-type/cost (:cost unit-def)
                     :unit-type/can-capture (:can-capture unit-def)
                     :unit-type/can-repair (map #(to-armor-type %) (:can-repair unit-def))
                     :unit-type/movement (:movement unit-def)
                     :unit-type/min-range (:min-range unit-def)
                     :unit-type/max-range (:max-range unit-def)
                     :unit-type/armor-type (-> unit-def :armor-type to-armor-type)
                     :unit-type/armor armor
                     :unit-type/capturing-armor (or capturing-armor armor)
                     :unit-type/repair (:repair unit-def)
                     :unit-type/state-map [:unit-state-map/game-id-idx unit-state-map-idx]
                     :unit-type/image (:image unit-def)
                     :unit-type/zoc-armor-types (map #(to-armor-type %) (:zoc unit-def))}]
                   (into (attack-strengths-tx db game-id unit-type-eid (:attack-strengths unit-def)))
                   (into (terrain-effects-tx db game-id unit-type-eid (:terrain-effects unit-def)))))))
          units-def)))

(defn unit-states-tx [db game-id state-map-eid state-map-name states]
  (let [game (game-by-id db game-id)]
    (into []
          (map
           (fn [[state-name states]]
             (let [state-id (to-unit-state-id state-map-name state-name)
                   state-idx (game-id-idx game-id state-id)]
               {:db/id (db/next-temp-id)
                :game/_unit-states (e game)
                :unit-state/id state-id
                :unit-state/game-id-idx state-idx
                :unit-state-map/_states state-map-eid})))
          states)))

(defn unit-states-transitions-tx [db game-id state-map-name states]
  (let [game (game-by-id db game-id)]
    (into []
          (mapcat
           (fn [[state-name {:keys [transitions]}]]
             (map
              (fn [[action new-state]]
                (let [state-idx (->> state-name (to-unit-state-id state-map-name) (game-id-idx game-id))
                      new-state-idx (->> new-state (to-unit-state-id state-map-name) (game-id-idx game-id))]
                  {:db/id (db/next-temp-id)
                   :unit-state-transition/action-type (to-action-type action)
                   :unit-state-transition/new-state [:unit-state/game-id-idx new-state-idx]
                   :unit-state/_transitions [:unit-state/game-id-idx state-idx]}))
              transitions)))
          states)))

(defn unit-state-map-tx [db game-id state-maps-def]
  (let [game (game-by-id db game-id)]
    (into []
          (mapcat
           (fn [[state-map-name state-map-def]]
             (let [{:keys [states start-state built-state]} state-map-def
                   map-id (to-unit-state-map-id state-map-name)
                   map-idx (game-id-idx game-id map-id)
                   start-idx (->> start-state (to-unit-state-id state-map-name) (game-id-idx game-id))
                   built-idx (->> built-state (to-unit-state-id state-map-name) (game-id-idx game-id))
                   state-map-temp-eid (db/next-temp-id)]
               (-> [{:db/id state-map-temp-eid
                     :game/_unit-state-maps (e game)
                     :unit-state-map/id map-id
                     :unit-state-map/game-id-idx map-idx}]
                   (into (unit-states-tx db game-id state-map-temp-eid state-map-name states))
                   (into (unit-states-transitions-tx db game-id state-map-name states))
                   (into [{:db/id state-map-temp-eid
                           :unit-state-map/start-state [:unit-state/game-id-idx start-idx]
                           :unit-state-map/built-state [:unit-state/game-id-idx built-idx]}])))))
          state-maps-def)))

(defn game-map-tx [db game-id map-def]
  (let [game (game-by-id db game-id)
        map-eid (db/next-temp-id)]
    (into [{:db/id map-eid
            :map/id (:id map-def)
            :map/description (:description map-def)
            :game/_map (e game)}]
          (map
           (fn [t]
             (let [{:keys [q r]} t
                   terrain-type-id (to-terrain-type-id (:terrain-type t))
                   terrain-type (terrain-type-by-id db game terrain-type-id)]
               {:db/id (db/next-temp-id)
                :terrain/game-pos-idx (game-pos-idx game q r)
                :terrain/q q
                :terrain/r r
                :terrain/type (e terrain-type)
                :map/_terrains map-eid}))
           (:terrains map-def)))))

(defn create-game!
  ([conn scenario-def]
   (create-game! conn scenario-def {}))
  ([conn scenario-def game-state]
   (let [game-id (random-uuid)
         {:keys [id credits-per-base max-count-per-unit]} scenario-def]
     (d/transact! conn [{:db/id (db/next-temp-id)
                         :game/id game-id
                         :game/scenario-id id
                         :game/round (:round game-state 1)
                         :game/max-count-per-unit max-count-per-unit
                         :game/credits-per-base credits-per-base}])
     game-id)))

(defn bases-tx [db game-id scenario-def]
  (let [game (game-by-id db game-id)]
    (for [base (:bases scenario-def)]
      (let [{:keys [q r]} base]
        {:terrain/game-pos-idx (game-pos-idx game q r)
         :terrain/q q
         :terrain/r r
         :terrain/type [:terrain-type/game-id-idx (game-id-idx game-id :terrain-type.id/base)]
         :map/_terrains (e (:game/map game))}))))

(defn factions-tx [db game-id factions]
  (let [game (game-by-id db game-id)]
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
                 factions)))

(defn factions-bases-tx [db game-id factions]
  (let [game (game-by-id db game-id)]
    (mapcat (fn [faction]
              (let [{:keys [bases color]} faction
                    faction (faction-by-color db game color)]
                (map (fn [{:keys [q r]}]
                       {:terrain/game-pos-idx (game-pos-idx game q r)
                        :terrain/owner (e faction)})
                     bases)))
            factions)))

(defn factions-units-tx [db game-id factions]
  (let [game (game-by-id db game-id)]
    (mapcat (fn [faction]
              (let [{:keys [game/max-count-per-unit]} game
                    {:keys [units color]} faction
                    faction-eid (e (faction-by-color db game color))]
                (map
                 (fn [{:keys [q r] :as unit}]
                   (let [unit-type-id (to-unit-type-id (:unit-type unit))
                         unit-state (:state unit)
                         unit-type (find-by db :unit-type/id unit-type-id)
                         capturing (:capturing unit false)
                         terrain (terrain-at db game q r)]
                     (cond-> {:db/id (db/next-temp-id)
                              :unit/game-pos-idx (game-pos-idx game q r)
                              :unit/q q
                              :unit/r r
                              :unit/terrain (e terrain)
                              :unit/count (:count unit max-count-per-unit)
                              :unit/round-built (:round-built unit 0)
                              :unit/move-count (:move-count unit 0)
                              :unit/attack-count (:attack-count unit 0)
                              :unit/attacked-count (:attack-count unit 0)
                              :unit/repaired (:repaired unit false)
                              :unit/capturing capturing
                              :unit/type (e unit-type)
                              :unit/state (if unit-state
                                            [:unit-state/game-id-idx (->> unit-state to-unit-state-id (game-id-idx game-id))]
                                            (-> unit-type start-state e))
                              :faction/_units faction-eid}
                       capturing
                       (assoc :unit/capture-round (:capture-round unit)))))
                 units)))
            factions)))

(defn load-scenario! [conn rulesets map-defs scenario-def]
  (let [game-id (create-game! conn scenario-def)
        {:keys [ruleset-id map-id credits-per-base factions]} scenario-def
        starting-faction-color (get-in factions [0 :color])
        ruleset (rulesets ruleset-id)]
    ;; Rules
    (d/transact! conn (settings-tx @conn game-id (:settings ruleset)))
    (d/transact! conn (terrain-types-tx @conn game-id (:terrains ruleset)))
    (d/transact! conn (unit-state-map-tx @conn game-id (:unit-state-maps ruleset)))
    (d/transact! conn (unit-types-tx @conn game-id (:units ruleset)))

    ;; Map and bases
    (d/transact! conn (game-map-tx @conn game-id (map-defs map-id)))
    (d/transact! conn (bases-tx @conn game-id scenario-def))

    ;; Factions
    (d/transact! conn (factions-tx @conn game-id factions))
    (d/transact! conn (factions-bases-tx @conn game-id factions))
    (d/transact! conn (factions-units-tx @conn game-id factions))
    (let [db @conn
          game (game-by-id db game-id)
          starting-faction-eid (->> starting-faction-color
                                    (faction-by-color db game)
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

(defn load-game-state! [conn rulesets map-defs scenario-defs game-state]
  (let [{:keys [scenario-id factions]} game-state
        current-faction-color (:current-faction game-state)
        scenario-def (scenario-defs scenario-id)
        starting-faction-color (get-in scenario-def [:factions 0 :color])
        game-id (create-game! conn scenario-def game-state)
        {:keys [ruleset-id map-id credits-per-base]} scenario-def
        ruleset (rulesets ruleset-id)]
    ;; Rules
    (d/transact! conn (settings-tx @conn game-id (:settings ruleset)))
    (d/transact! conn (terrain-types-tx @conn game-id (:terrains ruleset)))
    (d/transact! conn (unit-state-map-tx @conn game-id (:unit-state-maps ruleset)))
    (d/transact! conn (unit-types-tx @conn game-id (:units ruleset)))

    ;; Map and bases
    (d/transact! conn (game-map-tx @conn game-id (map-defs map-id)))
    (d/transact! conn (bases-tx @conn game-id scenario-def))

    ;; Factions
    (d/transact! conn (factions-tx @conn game-id factions))
    (d/transact! conn (factions-bases-tx @conn game-id factions))
    (d/transact! conn (factions-units-tx @conn game-id factions))
    (let [db @conn
          game (game-by-id db game-id)
          starting-faction-eid (e (faction-by-color db game starting-faction-color))
          current-faction-eid (e (faction-by-color db game current-faction-color))]
      (d/transact! conn [{:db/id (e game)
                          :game/starting-faction starting-faction-eid
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
                                :round-built (:unit/round-built unit)
                                :state (-> unit
                                           :unit/state
                                           :unit-state/id
                                           name
                                           keyword))
                         )))
               }))
      })))
