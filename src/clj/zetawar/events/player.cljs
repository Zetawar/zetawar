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

;; TODO: extract action => event logic into separate function
;; TODO: should faction-color be part of the event vector too?

(defmethod router/handle-event ::execute-action
  [{:as handler-ctx :keys [db]} [_ action]]
  (case (:action/type action)
    :action.type/build-unit
    (let [{:keys [action/q action/r action/unit-type-id]} action]
      {:dispatch [[:zetawar.events.game/build-unit q r unit-type-id]]})

    :action.type/move-unit
    (let [{:keys [action/from-q action/from-r
                  action/to-q action/to-r]} action]
      {:dispatch [[:zetawar.events.game/move-unit from-q from-r to-q to-r]]})

    :action.type/attack-unit
    (let [{:keys [action/attacker-q action/attacker-r
                  action/defender-q action/defender-r]} action]
      {:dispatch [[:zetawar.events.game/attack-unit
                   attacker-q attacker-r defender-q defender-r]]})

    :action.type/repair-unit
    (let [{:keys [action/q action/r]} action]
      {:dispatch [[:zetawar.events.game/repair-unit q r]]})

    :action.type/capture-base
    (let [{:keys [action/q action/r]} action]
      {:dispatch [[:zetawar.events.game/capture-base q r]]})))

(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [db]} _]
  (let [app (app/root db)]
    (when-not (:app/ai-turn-stepping app)
      {:dispatch [[:zetawar.events.game/end-turn]]})))
