(ns zetawar.events.game
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.game :as game]
   [zetawar.router :as router]))

(defmethod router/handle-event ::move-unit
  [{:as handler-ctx :keys [db]} [_ from-q from-r to-q to-r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    {:tx     (game/move-tx db (app/current-game db) from-q from-r to-q to-r)
     :notify [[:zetawar.players/apply-action :faction.color/all
               {:action/type :action.type/move
                :action/faction-color cur-faction-color
                :action/from-q from-q
                :action/from-r from-r
                :action/to-q to-q
                :action/to-r to-r}]]}))

(defmethod router/handle-event ::attack-unit
  [{:as handler-ctx :keys [db]} [_ attacker-q attacker-r defender-q defender-r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [attacker-damage defender-damage] (game/battle-damage db game
                                                              attacker-q attacker-r
                                                              defender-q defender-r)]
    {:tx     (game/battle-tx db game
                             attacker-q attacker-r
                             defender-q defender-r
                             attacker-damage
                             defender-damage)
     :notify [[:zetawar.players/apply-action :faction.color/all
               {:action/type :action.type/attack
                :action/faction-color cur-faction-color
                :action/attacker-q attacker-q
                :action/attacker-r attacker-r
                :action/defender-q defender-q
                :action/defender-r defender-r
                :action/attacker-damage attacker-damage
                :action/defender-damage defender-damage}]]}))

(defmethod router/handle-event ::repair-unit
  [{:as handler-ctx :keys [db]} [_ q r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [q r] (app/selected-hex db)]
    {:tx     (game/repair-tx db (app/current-game db) q r)
     :notify [[:zetawar.players/apply-action :faction.color/all
               {:action/type :action.type/repair-unit
                :action/faction-color cur-faction-color
                :action/q q
                :action/r r}]]}))

(defmethod router/handle-event ::capture-base
  [{:as handler-ctx :keys [db]} [_ q r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    {:tx     (game/capture-tx db (app/current-game db) q r)
     :notify [[:zetawar.players/apply-action :faction.color/all
               {:action/type :action.type/capture-base
                :action/faction-color cur-faction-color
                :action/q q
                :action/r r}]]}))

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} [_ q r unit-type-id]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    {:tx     (game/build-tx db (app/current-game db) q r unit-type-id)
     :notify [[:zetawar.players/apply-action :faction.color/all
               {:action/type :action.type/build-unit
                :action/faction-color cur-faction-color
                :action/q q
                :action/r r
                :action/unit-type-id unit-type-id}]]}))

(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        next-faction-color (game/next-faction-color game)]
    {:tx       (game/end-turn-tx db game)
     :dispatch [[:zetawar.events.ui/set-url-game-state]]
     :notify   [[:zetawar.players/apply-action :faction.color/all
                 {:action/type :action.type/end-turn
                  :action/faction-color cur-faction-color}]
                [:zetawar.players/start-turn next-faction-color]]}))
