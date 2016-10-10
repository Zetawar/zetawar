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
        game-state (game/get-game-state db game)]
    {:notify [[:zetawar.players/update-game-state faction-color game-state]]}))
