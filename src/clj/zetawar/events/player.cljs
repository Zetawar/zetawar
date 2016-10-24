(ns zetawar.events.player
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.game :as game]
   [zetawar.router :as router]))

(defmethod router/handle-event ::send-game-state
  [{:as handler-ctx :keys [db]} [_ faction-color]]
  (let [game (app/current-game db)
        game-state (game/get-game-state db game :full)]
    {:notify [[:zetawar.players/update-game-state faction-color game-state]]}))

(defmethod router/handle-event ::move-unit
  [{:as handler-ctx :keys [db]} [_ faction-color from-q from-r to-q to-r]]
  {:dispatch [[:zetawar.events.game/move-unit from-q from-r to-q to-r]]})

(defmethod router/handle-event ::attack-unit
  [{:as handler-ctx :keys [db]} [_ faction-color attacker-q attacker-r target-q target-r]]
  {:dispatch [[:zetawar.events.game/attack-unit attacker-q attacker-r target-q target-r]]})

(defmethod router/handle-event ::repair-unit
  [{:as handler-ctx :keys [db]} [_ faction-color q r]]
  {:dispatch [[:zetawar.events.game/repair-unit q r]]})

(defmethod router/handle-event ::capture-base
  [{:as handler-ctx :keys [db]} [_ faction-color q r]]
  {:dispatch [[:zetawar.events.game/capture-base q r]]})

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} [_ faction-color q r unit-type-id]]
  {:dispatch [[:zetawar.events.game/build-unit q r unit-type-id]]})

;; TODO: add execute-action

(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [db]} _]
  (let [app (app/root db)]
    (when-not (:app/ai-turn-stepping app)
      {:dispatch [[:zetawar.events.game/end-turn]]})))
