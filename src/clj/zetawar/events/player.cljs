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

;; TODO: should faction-color be part of the event vector too?

(defmethod router/handle-event ::execute-action
  [{:as handler-ctx :keys [db]} [_ action]]
  (let [app (app/root db)]
    (when-not (and (= (:action/type action) :action.type/end-turn)
                   (:app/ai-turn-stepping app))
      {:dispatch [[:zetawar.events.game/execute-action action]]})))
