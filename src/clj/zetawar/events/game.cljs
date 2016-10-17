(ns zetawar.events.game
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.game :as game]
   [zetawar.router :as router]))

(defmethod router/handle-event ::move-unit
  [{:as handler-ctx :keys [db]} [_ from-q from-r to-q to-r]]
  {:tx (game/move-tx db (app/current-game db) from-q from-r to-q to-r)
   :notify [[:zetawar.players/apply-action :faction.color/all
             :zetawar.actions/move from-q from-r to-q to-r]]})

(defmethod router/handle-event ::attack-unit
  [{:as handler-ctx :keys [db]} [_ attacker-q attacker-r defender-q defender-r]]
  (let [game (app/current-game db)
        damage (game/battle-damage db game attacker-q attacker-r defender-q defender-r)]
    (log/debug (pr-str damage))
    {:tx (game/battle-tx db (app/current-game db)
                         attacker-q attacker-r defender-q defender-r
                         damage)
     :notify [[:zetawar.players/apply-action :faction.color/all
               :zetawar.actions/attack attacker-q attacker-r defender-q defender-r
               (::game/attacker-damage damage) (::game/defender-damage damage)]]}))

(defmethod router/handle-event ::repair-unit
  [{:as handler-ctx :keys [db]} [_ q r]]
  (let [[q r] (app/selected-hex db)]
    {:tx (game/repair-tx db (app/current-game db) q r)
     :notify [[:zetawar.players/apply-action :faction.color/all
               :zetawar.actions/repair-unit q r]]}))

(defmethod router/handle-event ::capture-base
  [{:as handler-ctx :keys [db]} [_ q r]]
  {:tx (game/capture-tx db (app/current-game db) q r)
   :notify [[:zetawar.players/apply-action :faction.color/all
             :zetawar.actions/capture-base q r]]})

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} [_ q r unit-type-id]]
  {:tx (game/build-tx db (app/current-game db) q r unit-type-id)
   :notify [[:zetawar.players/apply-action :faction.color/all
             :zetawar.actions/build-unit q r unit-type-id]]})
