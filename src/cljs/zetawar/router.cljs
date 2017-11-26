(ns zetawar.router
  (:require
   [cljs.core.async :refer [<! >! chan offer!]]
   [cljsjs.raven]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.players :as players])
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]]))

(defmulti handle-event (fn [ev-ctx [ev-type & _]] ev-type))

(defmethod handle-event :default
  [_ msg]
  (let [log-msg (str "Unhandled event: " (pr-str msg))]
    (js/Raven.captureMessage log-msg)
    (log/warn log-msg))
  nil)

(defn dispatch [ch msg]
  (if msg
    (if (offer! ch msg)
      (log/debugf "Dispatching event: %s" (pr-str msg))
      (let [log-msg (str "Failed to dispatch event (buffer full?): " (pr-str msg))]
        (js/Raven.captureMessage log-msg)
        (log/error log-msg)))
    (let [log-msg "Unable to dispatch 'nil' event message"]
      (js/Raven.captureMessage log-msg)
      (log/error log-msg))))

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

(defn start [{:as router-ctx :keys [ev-chan handler-wrapper-fn max-render-interval]}]
  (let [handler-wrapper (if handler-wrapper-fn
                          (handler-wrapper-fn router-ctx)
                          (fn [handler] (handler)))]
    (go-loop []
      (when-let [msg (<! ev-chan)]
        (<! (handler-wrapper
             ;; Handle event
             #(try
                (log/debugf "Handling event: %s" (pr-str msg))
                (handle-event* router-ctx msg)
                (catch :default ex
                  (js/Raven.captureException ex)
                  (log/errorf ex "Error handling event: %s" (pr-str msg))))))
        (recur)))))
