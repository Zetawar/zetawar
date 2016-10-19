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
     :notify [[:zetawar.players/apply-action :faction.color/all cur-faction-color
               :zetawar.actions/move from-q from-r to-q to-r]]}))

(defmethod router/handle-event ::attack-unit
  [{:as handler-ctx :keys [db]} [_ attacker-q attacker-r defender-q defender-r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        damage (game/battle-damage db game attacker-q attacker-r defender-q defender-r)]
    {:tx     (game/battle-tx db (app/current-game db)
                             attacker-q attacker-r defender-q defender-r
                             damage)
     :notify [[:zetawar.players/apply-action :faction.color/all cur-faction-color
               :zetawar.actions/attack attacker-q attacker-r defender-q defender-r
               (::game/attacker-damage damage) (::game/defender-damage damage)]]}))

(defmethod router/handle-event ::repair-unit
  [{:as handler-ctx :keys [db]} [_ q r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [q r] (app/selected-hex db)]
    {:tx     (game/repair-tx db (app/current-game db) q r)
     :notify [[:zetawar.players/apply-action :faction.color/all cur-faction-color
               :zetawar.actions/repair-unit q r]]}))

(defmethod router/handle-event ::capture-base
  [{:as handler-ctx :keys [db]} [_ q r]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    {:tx     (game/capture-tx db (app/current-game db) q r)
     :notify [[:zetawar.players/apply-action :faction.color/all cur-faction-color
               :zetawar.actions/capture-base q r]]}))

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} [_ q r unit-type-id]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    {:tx     (game/build-tx db (app/current-game db) q r unit-type-id)
     :notify [[:zetawar.players/apply-action :faction.color/all cur-faction-color
               :zetawar.actions/build-unit q r unit-type-id]]}))

(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [db]} [_ q r unit-type-id]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        next-faction-color (game/next-faction-color game)]
    {:tx       (game/end-turn-tx db game)
     :dispatch [[:zetawar.events.ui/set-url-game-state]]
     :notify   [[:zetawar.players/apply-action :faction.color/all cur-faction-color
                 :zetawar.actions/end-turn]
                [:zetawar.players/start-turn next-faction-color]]}))
