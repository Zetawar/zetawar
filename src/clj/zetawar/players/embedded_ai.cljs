(ns zetawar.players.embedded-ai
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.players :as players]
   [zetawar.app :as app]
   [zetawar.game :as game])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

#_(defn handle-event* [{:as player-ctx :keys []} msg]
    (let [ev-ctx (assoc player-ctx :db @conn)
          {:as ret :keys [tx]} (handle-event ev-ctx msg)]
      (log/tracef "Player handler returned: %s" (pr-str ret))
      (when tx
        (log/debugf "Transacting: %s" (pr-str tx))
        (d/transact! conn tx))
      (doseq [new-msg (:dispatch ret)]
        (dispatch ev-chan new-msg))))


(defn new-player [{:as player-ctx :keys [notify-pub]} faction-color]
  (let [player-chan (async/chan (async/dropping-buffer 10))]
    (async/sub notify-pub :faction.color/all player-chan)
    (async/sub notify-pub faction-color player-chan)
    (go-loop [msg (<! player-chan)]
      (when msg
        (log/debugf "Handling player event: %s" (pr-str msg))
        ;; TODO: validate event
        ;; TODO: validate handler return value
        #_(handle-event* player-ctx msg)
        (recur (<! player-chan))))
    {:player-chan player-chan}))
