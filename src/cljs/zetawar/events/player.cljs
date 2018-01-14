(ns zetawar.events.player
  (:require
   [datascript.core :as d]
   [zetawar.app :as app]
   [zetawar.game :as game]
   [zetawar.logging :as log]
   [zetawar.router :as router]))

(defmethod router/handle-event ::send-game-state
  [{:as handler-ctx :keys [db]} [_ faction-color]]
  (let [game (app/current-game db)
        game-state (game/get-game-state db game :full)]
    {:notify [[:zetawar.players/update-game-state faction-color game-state]]}))

;; TODO: add middleware for actions?

(defmethod router/handle-event ::execute-action
  [{:as handler-ctx :keys [db]} [_ action]]
  (let [app (app/root db)
        game (app/current-game db)
        faction-color (game/current-faction-color game)
        action (assoc action :action/faction-color faction-color)]
    (when-not (and (= (:action/type action) :action.type/end-turn)
                   (:app/ai-turn-stepping app))
      {:dispatch [[:zetawar.events.game/execute-action action]]})))
