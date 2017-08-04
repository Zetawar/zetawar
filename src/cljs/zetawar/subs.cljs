(ns zetawar.subs
  (:require
   [datascript.core :as d]
   [posh.reagent :as posh]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [zetawar.db :refer [e qe]]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.tiles :as tiles]
   [zetawar.util :refer [breakpoint inspect select-values]])
  (:require-macros
   [zetawar.subs :refer [deftrack]]))

;; TODO: add param asserts to check params for better error messages?

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; App

(deftrack app-eid [conn]
  (ffirst @(posh/q '[:find ?a
                     :where
                     [?a :app/game]]
                   conn)))

(deftrack app [conn]
  @(posh/pull conn '[*] @(app-eid conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game

(deftrack game-eid [conn]
  (-> @(app conn) :app/game e))

(def game-pull [:game/id
                :game/self-repair
                :game/credits-per-base
                :game/max-count-per-unit
                :game/scenario-id
                {:game/map []}
                {:game/factions []}
                :game/starting-faction
                :game/current-faction
                :game/round])

(deftrack game [conn]
  @(posh/pull conn game-pull @(game-eid conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map

(deftrack game-map-eid [conn]
  (-> @(game conn) :game/map e))

(deftrack game-map [conn]
  @(posh/pull conn
              '[:map/description]
              @(game-map-eid conn)))

;; TODO: use terrains output instead of running a separate query
(deftrack terrain-eid-at [conn q r]
  (let [game-eid' @(game-eid conn)
        idx (game/game-pos-idx game-eid' q r)]
    (ffirst @(posh/q '[:find ?u
                       :in $ ?idx
                       :where
                       [?u :terrain/game-pos-idx ?idx]]
                     conn (game/game-pos-idx game-eid' q r)))))

(def terrain-pull [:terrain/q
                   :terrain/r
                   {:terrain/type [:terrain-type/id
                                   :terrain-type/image]
                    :terrain/owner [:faction/color]}])

;; TODO: use terrains output instead of running a separate query
(deftrack terrain-at [conn q r]
  (when-let [terrain-eid @(terrain-eid-at conn q r)]
    @(posh/pull conn terrain-pull terrain-eid)))

(deftrack terrains [conn]
  (let [map-eid' @(game-map-eid conn)]
    (:map/terrains
     @(posh/pull conn [{:map/terrains terrain-pull}]
                 map-eid'))))

(deftrack map-width [conn]
  (or (->> @(terrains conn)
           (map :terrain/q)
           (apply max))
      0))

(deftrack map-height [conn]
  (or (->> @(terrains conn)
           (map :terrain/r)
           (apply max))
      0))

(deftrack map-width-px [conn]
  (+ tiles/odd-row-column-offset
     (* tiles/width
        (inc @(map-width conn)))))

(deftrack map-height-px [conn]
  (+ tiles/height
     (* tiles/row-offset
        @(map-height conn))))

;; TODO: use terrains output instead of running a separate query
(defn current-base-locations [conn]
  (posh/q '[:find ?q ?r
            :where
            [_  :app/game ?g]
            [?g :game/map ?m]
            [?g :game/current-faction ?f]
            [?t :terrain/owner ?f]
            [?m :map/terrains ?t]
            [?t :terrain/q ?q]
            [?t :terrain/r ?r]]
          conn
          {:cache :forever}))

(deftrack current-base? [conn q r]
  (contains? @(current-base-locations conn) [q r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Factions

(deftrack faction-eids [conn]
  (into []
        (map e)
        (:game/factions @(game conn))))

(def faction-pull '[:faction/color
                    :faction/credits
                    :faction/player-type
                    :faction/ai
                    :faction/next-faction
                    :faction/order])

(deftrack factions-by-eid [conn]
  (->> @(faction-eids conn)
       (map (fn [eid] [eid @(posh/pull conn faction-pull eid)]))
       (into {})))

(deftrack factions [conn]
  (->> @(factions-by-eid conn)
       (map second)
       (sort-by :faction/order)
       (into [])))

(deftrack faction-eid->base-count [conn]
  (->> @(posh/q '[:find ?f (count ?t)
                  :where
                  [_  :app/game ?g]
                  [?g :game/factions ?f]
                  [?t :terrain/owner ?f]]
                conn)
       (into {})))

(deftrack faction-eid->base-being-captured-count [conn]
  (->> @(posh/q '[:find ?f (count ?t)
                  :where
                  [_ :app/game ?g]
                  [?g :game/factions ?f]
                  [?t :terrain/owner ?f]
                  [(not= ?ef f)]
                  [?ef :faction/units ?u]
                  [?u :unit/terrain ?t]
                  [?u :unit/capturing true]]
                conn)
       (into {})))

(deftrack faction-eid->unit-count [conn]
  (->> @(posh/q '[:find ?f (count ?u)
                  :where
                  [_  :app/game ?g]
                  [?g :game/factions ?f]
                  [?f :faction/units ?u]]
                conn)
       (into {})))

(deftrack winning-faction-eid [conn]
  (let [faction-eids-with-bases (into []
                                      (comp (filter #(> (second %) 0))
                                            (map first))
                                      @(faction-eid->base-count conn))
        faction-eids-with-units (into []
                                      (comp (filter #(> (second %) 0))
                                            (map first))
                                      @(faction-eid->unit-count conn))]
    (when (and (= (count faction-eids-with-bases) 1)
               (= (count faction-eids-with-units) 1)
               (= faction-eids-with-bases faction-eids-with-units))
      (first faction-eids-with-bases))))

(deftrack current-faction-eid [conn]
  (e (:game/current-faction @(game conn))))

(defn current-faction [conn]
  (posh/pull conn faction-pull @(current-faction-eid conn)))

(deftrack current-faction-won? [conn]
  (= @(current-faction-eid conn)
     @(winning-faction-eid conn)))

(deftrack current-base-count [conn]
  (get @(faction-eid->base-count conn) @(current-faction-eid conn)))

(deftrack current-base-being-captured-count [conn]
  (get @(faction-eid->base-being-captured-count conn) @(current-faction-eid conn)))

(deftrack current-unit-count [conn]
  (get @(faction-eid->unit-count conn) @(current-faction-eid conn)))

(deftrack current-income [conn]
  (let [{:keys [game/credits-per-base]} @(game conn)]
    (* credits-per-base
       (- @(current-base-count conn)
          @(current-base-being-captured-count conn)))))

(deftrack enemy-unit-count [conn]
  (or (ffirst @(posh/q '[:find (count ?u)
                         :in $ ?cf
                         :where
                         [?f :faction/units ?u]
                         [(not= ?f ?cf)]]
                       conn @(current-faction-eid conn)))
      0))

(deftrack enemy-base-count [conn]
  (or (ffirst @(posh/q '[:find (count ?b)
                         :in $ ?cf
                         :where
                         [?b :terrain/owner ?f]
                         [(not= ?f ?cf)]]
                       conn @(current-faction-eid conn)))
      0))

(deftrack faction-color-name [faction]
  (when @faction
    (-> @faction
        :faction/color
        name
        (str "-name")
        keyword)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Units

(deftrack unit-eid-at [conn q r]
  (let [game-eid' @(game-eid conn)
        idx (game/game-pos-idx game-eid' q r)]
    (ffirst @(posh/q '[:find ?u
                       :in $ ?idx
                       :where
                       [?u :unit/game-pos-idx ?idx]]
                     conn idx))))

(deftrack unit-at? [conn q r]
  (some? @(unit-eid-at conn q r)))

(deftrack unit-at [conn q r]
  (when-let [unit-eid @(unit-eid-at conn q r)]
    @(posh/pull conn '[:unit/q
                       :unit/r
                       :unit/round-built
                       :unit/move-count
                       :unit/attack-count
                       :unit/count
                       :unit/repaired
                       :unit/capturing
                       {:faction/_units [:faction/color]
                        :unit/type [:unit-type/id
                                    :unit-type/description
                                    :unit-type/can-capture
                                    :unit-type/can-repair
                                    :unit-type/armor-type
                                    :unit-type/min-range
                                    :unit-type/max-range
                                    :unit-type/image]}]
                unit-eid)))

(deftrack current-unit-at [conn q r]
  (when-let [unit @(unit-at conn q r)]
    (let [cur-faction-eid @(current-faction-eid conn)]
      (when (= cur-faction-eid (e (:faction/_units unit)))
        unit))))

(deftrack current-unit-eid-at [conn q r]
  (some-> @(current-unit-at conn q r) e))

(deftrack current-unit-at? [conn q r]
  (some? @(current-unit-at conn q r)))

(deftrack unit-color-at [conn q r]
  (get-in @(unit-at conn q r) [:faction/_units :faction/color]))

(deftrack unit-type-at [conn q r]
  (get-in @(unit-at conn q r) [:unit/type :unit-type/id]))

(deftrack enemy-locations [conn]
  @(posh/q '[:find ?q ?r
             :in $ ?g ?cf
             :where
             [_  :app/game ?g]
             [?g :game/factions ?f]
             [?f :faction/units ?u]
             [?u :unit/q ?q]
             [?u :unit/r ?r]
             [(not= ?f ?cf)]]
           conn @(game-eid conn) @(current-faction-eid conn)
           {:cache :forever}))

(deftrack enemy-at? [conn q r]
  (contains? @(enemy-locations conn) [q r]))

(deftrack enemy-locations-in-range-of [conn q r]
  (let [attacker @(unit-at conn q r)
        min-range (get-in attacker [:unit/type :unit-type/min-range])
        max-range (get-in attacker [:unit/type :unit-type/max-range])]
    (into #{}
          (filter #(let [distance (apply hex/distance q r %)]
                     (and (>= distance min-range) (<= distance max-range))))
          @(enemy-locations conn))))

(deftrack any-enemy-in-range-of? [conn q r]
  (not (empty? @(enemy-locations-in-range-of conn q r))))

(deftrack in-range-of-enemy-at? [conn unit-q unit-r enemy-q enemy-r]
  (contains? @(enemy-locations-in-range-of conn unit-q unit-r) [enemy-q enemy-r]))

(deftrack friend-locations [conn]
  @(posh/q '[:find ?q ?r
             :in $ ?g ?cf
             :where
             [_  :app/game ?g]
             [?g :game/factions ?f]
             [?f :faction/units ?u]
             [?u :unit/q ?q]
             [?u :unit/r ?r]
             [(= ?f ?cf)]]
           conn @(game-eid conn) @(current-faction-eid conn)
           {:cache :forever}))

(deftrack friend-at? [conn q r]
  (contains? @(friend-locations conn) [q r]))

(deftrack friend-locations-in-range-of [conn q r]
  (let [unit @(unit-at conn q r)
        min-range (get-in unit [:unit/type :unit-type/min-range])
        max-range (get-in unit [:unit/type :unit-type/max-range])]
    (into #{}
          (filter #(let [distance (apply hex/distance q r %)]
                     (and (>= distance min-range) (<= distance max-range))))
          @(friend-locations conn))))

(deftrack any-friend-in-range-of? [conn q r]
  (not (empty? @(friend-locations-in-range-of conn q r))))

(deftrack in-range-of-friend-at? [conn unit-q unit-r friend-q friend-r]
  (contains? @(friend-locations-in-range-of conn unit-q unit-r) [friend-q friend-r]))

(deftrack unit-terrain-effects [conn unit-q unit-r terrain-q terrain-r]
  (when-let [unit @(unit-at conn unit-q unit-r)]
    (let [terrain @(terrain-at conn terrain-q terrain-r)]
      (game/unit-terrain-effects @conn unit terrain))))

(deftrack repairable? [conn q r]
  (when-let [unit @(unit-at conn q r)]
    (game/repairable? @conn @(game conn) unit)))

(deftrack can-move? [conn q r]
  (when-let [unit @(unit-at conn q r)]
    (game/can-move? @conn @(game conn) unit)))

(deftrack can-attack? [conn q r]
  (when-let [unit @(unit-at conn q r)]
    (and (game/can-attack? @conn @(game conn) unit)
         @(any-enemy-in-range-of? conn q r))))

(deftrack can-repair? [conn q r]
  (when-let [unit @(unit-at conn q r)]
    (game/can-repair? @conn @(game conn) unit)))

(deftrack can-field-repair? [conn q r]
  (when-let [unit @(unit-at conn q r)]
    (game/can-field-repair? @conn @(game conn) unit)))

(deftrack can-capture? [conn q r]
  (let [unit @(unit-at conn q r)
        terrain @(terrain-at conn q r)]
    (and unit
         terrain
         (game/can-capture? @conn @(game conn) unit terrain))))

(deftrack unit-can-act? [conn q r]
  (or @(can-move? conn q r)
      @(can-attack? conn q r)
      @(can-repair? conn q r)
      @(can-field-repair? conn q r)
      @(can-capture? conn q r)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Unit construction

(deftrack available-unit-type-eids [conn]
  (->> @(posh/q '[:find ?ut
                  :in $ ?g
                  :where
                  [_   :app/game ?g]
                  [?g  :game/current-faction ?f]
                  [?f  :faction/credits ?credits]
                  [?ut :unit-type/cost ?cost]
                  [?ut :unit-type/id ?unit-type-id]]
                conn @(game-eid conn)
                {:cache :forever})
       (map first)
       (into [])))

(deftrack available-unit-types [conn]
  (let [{:keys [faction/credits]} @(current-faction conn)]
    (->> @(available-unit-type-eids conn)
         (map (fn [ut-eid]
                (let [ut @(posh/pull conn '[*] ut-eid)
                      affordable (<= (:unit-type/cost ut) credits)]
                  ;; TODO: make affordable a namespaced key (?)
                  (assoc ut :affordable affordable))))
         (sort-by :unit-type/cost)
         (into []))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Selection and target

(deftrack selected? [conn q r]
  (let [app' @(app conn)]
    (and (= q (:app/selected-q app'))
         (= r (:app/selected-r app')))))

(deftrack targeted? [conn q r]
  (let [app' @(app conn)]
    (and (= q (:app/targeted-q app'))
         (= r (:app/targeted-r app')))))

(deftrack selected-hex [conn]
  (-> @(app conn)
      (select-values [:app/selected-q
                      :app/selected-r])
      not-empty))

(deftrack targeted-hex [conn]
  (-> @(app conn)
      (select-values [:app/targeted-q
                      :app/targeted-r])
      not-empty))

(deftrack selected-unit [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(unit-at conn q r)))

(deftrack selected-terrain-effects [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(unit-terrain-effects conn q r q r)))

(deftrack targeted-terrain-effects [conn]
  (when-let [[terrain-q terrain-r] @(targeted-hex conn)]
    (let [[unit-q unit-r] @(selected-hex conn)]
      @(unit-terrain-effects conn unit-q unit-r terrain-q terrain-r))))

(deftrack unit-selected? [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(unit-at? conn q r)))

(deftrack selected-can-move? [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(can-move? conn q r)))

(deftrack selected-can-attack? [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(can-attack? conn q r)))

(deftrack selected-can-repair? [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(can-repair? conn q r)))

(deftrack selected-can-field-repair? [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(can-field-repair? conn q r)))

(deftrack has-repairable-armor-type? [conn targeted-q targeted-r]
  (when-let [[selected-q selected-r] @(selected-hex conn)]
    (game/has-repairable-armor-type? @conn @(game conn)
                                     @(unit-at conn selected-q selected-r)
                                     @(unit-at conn targeted-q targeted-r))))

(deftrack selected-can-capture? [conn]
  (when-let [[q r] @(selected-hex conn)]
    @(can-capture? conn q r)))

(deftrack selected-can-build? [conn]
  (when-let [[q r] @(selected-hex conn)]
    (and (not @(unit-selected? conn))
         @(current-base? conn q r))))

(deftrack valid-destinations-for-selected [conn]
  (if @(selected-can-move? conn)
    (let [db @conn
          game' (d/entity db @(game-eid conn))
          [q r] @(selected-hex conn)
          unit (game/unit-at db game' q r)]
      (game/valid-destinations db game' unit))
    #{}))

(deftrack valid-destination-for-selected? [conn q r]
  (contains? @(valid-destinations-for-selected conn) [q r]))

(deftrack selected-can-move-to-targeted? [conn]
  (when-let [[q r] @(targeted-hex conn)]
    (and @(selected-can-move? conn)
         (contains? @(valid-destinations-for-selected conn) [q r]))))

(deftrack enemy-in-range-of-selected? [conn q r]
  (when-let [[selected-q selected-r] @(selected-hex conn)]
    @(in-range-of-enemy-at? conn selected-q selected-r q r)))

(deftrack friend-in-range-of-selected? [conn q r]
  (when-let [[selected-q selected-r] @(selected-hex conn)]
    @(in-range-of-friend-at? conn selected-q selected-r q r)))

(deftrack repairable-friend-in-range-of-selected? [conn q r]
  (when-let [[selected-q selected-r] @(selected-hex conn)]
    (and @(in-range-of-friend-at? conn selected-q selected-r q r)
         @(repairable? conn q r))))

(deftrack selected-can-attack-targeted? [conn]
  (when-let [[q r] @(targeted-hex conn)]
    (and @(selected-can-attack? conn)
         @(enemy-in-range-of-selected? conn q r))))

(deftrack selected-can-repair-targeted? [conn]
  (when-let [[targeted-q targeted-r] @(targeted-hex conn)]
    (and @(selected-can-field-repair? conn)
         @(friend-in-range-of-selected? conn targeted-q targeted-r)
         @(repairable? conn targeted-q targeted-r)
         @(has-repairable-armor-type? conn targeted-q targeted-r))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Unit picker

(deftrack picking-unit? [conn]
  (:app/picking-unit @(app conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Win message

(deftrack show-win-message? [conn]
  (and @(current-faction-won? conn)
       (not (:faction/ai @(current-faction conn)))
       (not (:app/hide-win-message @(app conn)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Faction configuration

(deftrack faction-to-configure [conn]
  (some->> @(app conn)
           :app/configuring-faction
           e
           (get @(factions-by-eid conn))))

(deftrack configuring-faction? [conn]
  (some? @(faction-to-configure conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; New game configuration

(deftrack configuring-new-game? [conn]
  (:app/configuring-new-game @(app conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Tile coordinates

(deftrack hover-hex [conn]
  (-> @(app conn)
      (select-values [:app/hover-q
                      :app/hover-r])
      not-empty))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; End turn

(deftrack available-moves-left? [conn]
  (some
   (fn [[q r] coordinates] @(unit-can-act? conn q r))
   @(friend-locations conn)))

(deftrack show-end-turn-alert? [conn]
  (:app/end-turn-alert @(app conn)))
