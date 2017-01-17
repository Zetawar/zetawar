(ns zetawar.router
  (:require
   [cljs.core.async :refer [chan close! put!]]
   [cljsjs.raven]
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

;; TODO: add spec for dispatch
(defn dispatch [ch msg]
  (log/debugf "Dispatching event: %s" (pr-str msg))
  (put! ch msg))

;; TODO: add pause for rendering after fixed interval
(defn handle-event* [{:as router-ctx :keys [conn ev-chan notify-chan]} msg]
  (let [ev-ctx (assoc router-ctx :db @conn)
        {:as ret :keys [tx]} (handle-event ev-ctx msg)]
    (log/tracef "Handler returned: %s" (pr-str ret))
    (when tx
      (log/debugf "Transacting: %s" (pr-str tx))
      (d/transact! conn tx))
    (doseq [new-msg (:dispatch ret)]
      (dispatch ev-chan new-msg))
    ;; TODO: block with timeout when notifying players
    (doseq [notify-msg (:notify ret)]
      (players/notify notify-chan notify-msg))))

(defn start [{:as router-ctx :keys [ev-chan]}]
  (go-loop [msg (<! ev-chan)]
    (when msg
      (try
        (log/debugf "Handling event: %s" (pr-str msg))
        ;; TODO: validate event
        ;; TODO: validate handler return value
        (handle-event* router-ctx msg)
        (throw (js/Error. "testing"))
        (catch :default ex
          (js/Raven.captureException ex)
          (log/errorf ex "Error handling event: %s" (pr-str msg))))
      (recur (<! ev-chan)))))
