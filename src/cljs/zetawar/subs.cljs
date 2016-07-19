(ns zetawar.subs
  (:require
    [datascript.core :as d]
    [zetawar.game :as game]
    [zetawar.hex :as hex]
    [zetawar.util :refer [e qe select-values spy]]
    [posh.core :as posh]
    [reagent.core :as r])
  (:require-macros
    [zetawar.subs :refer [deftrack]]))

;; TODO: add asserts to check params for better error messages?

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; App

(deftrack app-eid [conn]
  (ffirst
    @(posh/q conn '[:find ?a
                    :where
                    [?a :app/game]])))

(defn app [conn]
  (posh/pull conn '[*] @(app-eid conn)))

(deftrack show-win-dialog? [conn]
  (:app/show-win-dialog @(app conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game

(deftrack game-eid [conn]
  (ffirst
    @(posh/q conn '[:find ?g
                    :where
                    [_ :app/game ?g]])))

(defn game [conn]
  (posh/pull conn '[*] @(game-eid conn)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map

(deftrack game-map-eid [conn]
  (ffirst
    @(posh/q conn '[:find ?m
                    :where
                    [_  :app/game ?g]
                    [?g :game/map ?m]])))

(defn game-map [conn]
  (posh/pull conn
             '[:map/credits-per-base
               :map/name
               :map/starting-faction]
             @(game-map-eid conn)))

(deftrack terrain-eid-at [conn q r]
  (let [game-eid' @(game-eid conn)
        idx (game/game-pos-idx game-eid' q r)]
    (ffirst @(posh/q conn '[:find ?u
                            :in $ ?idx
                            :where
                            [?u :terrain/game-pos-idx ?idx]]
                     (game/game-pos-idx game-eid' q r)))))

(def terrain-pull [:terrain/q
                   :terrain/r
                   {:terrain/type [:terrain-type/id
                                   :terrain-type/image]
                    :terrain/owner [:faction/color]}])

(deftrack terrain-at [conn q r]
  (when-let [terrain-eid @(terrain-eid-at conn q r)]
    @(posh/pull conn terrain-pull terrain-eid)))

(deftrack terrains [conn]
  (let [map-eid' @(game-map-eid conn)]
    (:map/terrains
      @(posh/pull conn [{:map/terrains terrain-pull}]
                  map-eid'))))

(defn current-base-locations [conn]
  (posh/q conn '[:find ?q ?r
                 :where
                 [_  :app/game ?g]
                 [?g :game/map ?m]
                 [?g :game/current-faction ?f]
                 [?t :terrain/owner ?f]
                 [?m :map/terrains ?t]
                 [?t :terrain/q ?q]
                 [?t :terrain/r ?r]]))

(deftrack current-base? [conn q r]
  (contains? @(current-base-locations conn) [q r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Factions

(deftrack faction-eids [conn]
  (->> @(posh/q conn '[:find ?f ?o
                       :where
                       [_  :app/game ?g]
                       [?g :game/factions ?f]
                       [?f :faction/order ?o]])
       (sort-by second)
       (map first)
       (into [])))

(def faction-pull '[:faction/color
                    :faction/credits
                    :faction/ai
                    :faction/next-faction])

(deftrack factions [conn]
  (->> @(faction-eids conn)
       (map (fn [f] @(posh/pull conn faction-pull f)))
       (into [])))

(deftrack current-faction-eid [conn]
  (ffirst
    @(posh/q conn '[:find ?f
                    :where
                    [_  :app/game ?g]
                    [?g :game/current-faction ?f]])))

(defn current-faction [conn]
  (posh/pull conn faction-pull @(current-faction-eid conn)))

(deftrack current-unit-count [conn]
  (or (ffirst @(posh/q conn '[:find (count ?u)
                              :in $ ?f
                              :where
                              [?f :faction/units ?u]]
                       @(current-faction-eid conn)))
      0))

(deftrack current-base-count [conn]
  (or (ffirst @(posh/q conn '[:find (count ?b)
                              :in $ ?f
                              :where
                              [?b :terrain/owner ?f]]
                       @(current-faction-eid conn)))
      0))

(deftrack current-income [conn]
  (let [{:keys [map/credits-per-base]} @(game-map conn)]
    (* (:map/credits-per-base @(game-map conn))
       @(current-base-count conn @(current-faction-eid conn)))))

(deftrack enemy-unit-count [conn]
  (or (ffirst @(posh/q conn '[:find (count ?u)
                              :in $ ?cf
                              :where
                              [?f :faction/units ?u]
                              [(not= ?f ?cf)]]
                       @(current-faction-eid conn)))
      0))

(deftrack enemy-base-count [conn]
  (or (ffirst @(posh/q conn '[:find (count ?b)
                              :in $ ?cf
                              :where
                              [?b :terrain/owner ?f]
                              [(not= ?f ?cf)]]
                       @(current-faction-eid conn)))
      0))

;; TODO: move to units section?
;; TODO: sort by cost
(deftrack buildable-unit-type-eids [conn]
  (->> @(posh/q conn '[:find ?ut
                       :in $ ?g
                       :where
                       [?g  :game/current-faction ?f]
                       [?f  :faction/credits ?credits]
                       [?ut :unit-type/cost ?cost]
                       [?ut :unit-type/id ?unit-type-id]
                       [(>= ?credits ?cost)]]
                @(game-eid conn))
       (map first)
       (into [])))

;; TODO: move to units section?
(deftrack buildable-unit-types [conn]
  (->> @(buildable-unit-type-eids conn)
       (map (fn [ut] @(posh/pull conn '[*] ut)))
       (into [])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Units

(deftrack unit-eid-at [conn q r]
  (let [game-eid' @(game-eid conn)
        idx (game/game-pos-idx game-eid' q r)]
    (ffirst @(posh/q conn '[:find ?u
                            :in $ ?idx
                            :where
                            [?u :unit/game-pos-idx ?idx]]
                     (game/game-pos-idx game-eid' q r)))))

(deftrack current-unit-eid-at [conn q r]
  (let [game-eid' @(game-eid conn)]
    (ffirst @(posh/q conn '[:find ?u
                            :in $ ?g ?idx
                            :where
                            [?u :unit/game-pos-idx ?idx]
                            [?f :faction/units ?u]
                            [?g :game/current-faction ?f]]
                     game-eid' (game/game-pos-idx game-eid' q r)))))

(deftrack unit-at? [conn q r]
  (some? @(unit-eid-at conn q r)))

(deftrack current-unit-at? [conn q r]
  (some? @(current-unit-eid-at conn q r)))

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
                                    :unit-type/name
                                    :unit-type/can-capture
                                    :unit-type/min-range
                                    :unit-type/max-range
                                    :unit-type/image]}]
                unit-eid)))

(deftrack unit-color-at [conn q r]
  (get-in @(unit-at conn q r) [:faction/_units :faction/color]))

(deftrack unit-type-at [conn q r]
  (get-in @(unit-at conn q r) [:unit/type :unit-type/id]))

(deftrack enemy-locations [conn]
  @(posh/q conn '[:find ?q ?r
                  :in $ ?g ?cf
                  :where
                  [?g :game/factions ?f]
                  [?f :faction/units ?u]
                  [?u :unit/q ?q]
                  [?u :unit/r ?r]
                  [(not= ?f ?cf)]]
           @(game-eid conn) @(current-faction-eid conn)))

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
      @(can-capture? conn q r)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Selection and Target

(deftrack selected? [conn q r]
  (let [app' @(app conn)]
    (and (= q (:app/selected-q app'))
         (= r (:app/selected-r app')))))

(deftrack targeted? [conn q r]
  (let [app' @(app conn)]
    (and (= q (:app/targeted-q app'))
         (= r (:app/targeted-r app')))))

(deftrack selected-qr [conn]
  (-> @(app conn)
      (select-values [:app/selected-q
                      :app/selected-r])
      not-empty))

(deftrack targeted-qr [conn]
  (-> @(app conn)
      (select-values [:app/targeted-q
                      :app/targeted-r])
      not-empty))

(deftrack selected-unit [conn]
  @(apply unit-at conn @(selected-qr conn)))

(deftrack unit-selected? [conn]
  @(apply unit-at? conn @(selected-qr conn)))

;; TODO: these should be deftracks
(deftrack selected-can-move? [conn]
  (apply can-move? conn @(selected-qr conn)))

(deftrack selected-can-attack? [conn]
  (apply can-attack? conn @(selected-qr conn)))

(deftrack selected-can-repair? [conn]
  (apply can-repair? conn @(selected-qr conn)))

(deftrack selected-can-capture? [conn]
  (apply can-capture? conn @(selected-qr conn)))

(deftrack selected-can-build? [conn q r]
  (let [[q r] @(selected-qr conn q r)]
    (and (not @(unit-selected? conn q r))
         @(current-base? conn q r))))

(deftrack valid-destinations-for-selected [conn]
  (if @(selected-can-move? conn)
    (let [db @conn
          game' (d/entity db @(game-eid conn))
          [q r] @(selected-qr conn)
          unit (game/unit-at db game' q r)]
      (game/valid-destinations db game' unit))
    #{}))

(deftrack valid-destination-for-selected? [conn q r]
  (contains? @(valid-destinations-for-selected conn) [q r]))

(deftrack selected-can-move-to-targeted? [conn]
  (let [[tq tr] @(targeted-qr conn)]
    (and @(selected-can-move? conn)
         (contains? @(valid-destinations-for-selected conn) [tq tr]))))

(deftrack enemy-in-range-of-selected? [conn q r]
  (let [[selected-q selected-r] @(selected-qr conn)]
    @(in-range-of-enemy-at? conn selected-q selected-r q r)))

(deftrack selected-can-attack-targeted? [conn]
  (let [[tq tr] @(targeted-qr conn)]
    (and @(selected-can-attack? conn)
         @(enemy-in-range-of-selected? conn tq tr))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; User Interface
