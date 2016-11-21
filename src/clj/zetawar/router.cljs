(ns zetawar.router
  (:require
   [cljs.core.async :refer [chan close! put!]]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.players :as players])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defmulti handle-event (fn [ev-ctx [ev-type & _]] ev-type))

(defmethod handle-event :default
  [_ msg]
  (log/debugf "Unhandled event: %s" (pr-str msg))
  nil)

(defn dispatch [ch msg]
  (log/debugf "Dispatching event: %s" (pr-str msg))
  (put! ch msg))

;; TODO: handle exceptions
(defn handle-event* [{:as router-ctx :keys [conn ev-chan notify-chan]} msg]
  (let [ev-ctx (assoc router-ctx :db @conn)
        {:as ret :keys [tx]} (handle-event ev-ctx msg)]
    (log/tracef "Handler returned: %s" (pr-str ret))
    (when tx
      (log/debugf "Transacting: %s" (pr-str tx))
      (d/transact! conn tx))
    (doseq [new-msg (:dispatch ret)]
      (dispatch ev-chan new-msg))
    (doseq [notify-msg (:notify ret)]
      (players/notify notify-chan notify-msg))))

(defn start [{:as router-ctx :keys [ev-chan]}]
  (go-loop [msg (<! ev-chan)]
    (when msg
      (log/debugf "Handling event: %s" (pr-str msg))
      ;; TODO: validate event
      ;; TODO: validate handler return value
      (handle-event* router-ctx msg)
      (recur (<! ev-chan)))))
