(ns zetawar.players.embedded-ai
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.data :as data]
   [zetawar.db :as db]
   [zetawar.game :as game]
   [zetawar.players :as players]
   [zetawar.router :as router])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defmulti handle-event (fn [player [ev-type & _]] ev-type))

(defmethod handle-event :default
  [_ msg]
  (log/debugf "Unhandled player event: %s" (pr-str msg)))

(defn handle-event* [{:as player :keys [ev-chan]} msg]
  (let [{:as ret :keys [tx]} (handle-event player msg)]
    (log/tracef "Player handler returned: %s" (pr-str ret))
    (doseq [new-msg (:dispatch ret)]
      (router/dispatch ev-chan new-msg))))

(defrecord EmbeddedAIPlayer [faction-color ev-chan notify-pub player-chan conn]
  players/Player
  (start [player]
    (let [{:keys [notify-pub]} player]
      (async/sub notify-pub :faction.color/all player-chan)
      (async/sub notify-pub faction-color player-chan)
      (go-loop [msg (<! player-chan)]
        (when msg
          (log/debugf "Handling player event: %s" (pr-str msg))
          ;; TODO: validate event
          ;; TODO: validate handler return value
          (handle-event* player msg)
          (recur (<! player-chan))))))
  (stop [player]
    (async/close! player-chan)))

(defmethod players/new-player ::players/embeded-ai
  [{:as app-ctx :keys [ev-chan notify-pub]} player-type faction-color]
  (let [player-chan (async/chan (async/dropping-buffer 10))]
    (EmbeddedAIPlayer. faction-color ev-chan notify-pub player-chan (d/create-conn db/schema))))

(defmethod handle-event ::players/start-turn
  [{:as player :keys [conn]} _]
  {:dispatch [[:zetawar.events.player/send-game-state (:faction-color player)]]})

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn]} [_ faction-color game-state]]
  (when (= faction-color (:faction-color player))
    (let [new-conn (d/create-conn db/schema)]
      (game/load-specs! new-conn)
      (game/load-game-state! new-conn data/map-definitions data/scenario-definitions game-state)
      (reset! conn @new-conn)

      ;; build units if possible

      ;; select unit to move
      ;; move unit
      ;; attack if possible

      {}

      )))
