(ns zetawar.router
  (:require
   [cljs.core.async :refer [<! >! chan offer!]]
   [datascript.core :as d]
   [goog.object :as gobj]
   [zetawar.logging :as log]
   [zetawar.players :as players])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(when-not (exists? js/Raven)
  (let [stub #js {"captureMessage" #()
                  ;; TODO: something better than js/console.trace
                  "captureException" #(js/console.trace)}
        global (if (exists? js/window) js/window js/global)]
    (gobj/set global "Raven" stub)))

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
      (log/info "Dispatching event:" (pr-str msg))
      (let [log-msg (str "Failed to dispatch event (buffer full?): " (pr-str msg))]
        (js/Raven.captureMessage log-msg)
        (log/error log-msg)))
    (let [log-msg "Unable to dispatch 'nil' event message"]
      (js/Raven.captureMessage log-msg)
      (log/error log-msg))))

(defn handle-event* [{:as router-ctx :keys [conn ev-chan notify-chan]} msg]
  (let [ev-ctx (assoc router-ctx :db @conn)
        {:as ret :keys [tx]} (handle-event ev-ctx msg)]
    (log/trace "Handler returned:" (pr-str ret))
    (when tx
      (log/debug "Transacting:" (pr-str tx))
      (d/transact! conn tx))
    (doseq [new-msg (:dispatch ret)]
      (dispatch ev-chan new-msg))
    ;; TODO: block with timeout when notifying players
    (doseq [notify-msg (:notify ret)]
      (players/notify notify-chan notify-msg))))

(defn start [{:as router-ctx :keys [ev-chan handler-wrapper-fn max-render-interval]}]
  (let [handler-wrapper (if handler-wrapper-fn
                          (handler-wrapper-fn router-ctx)
                          (fn [handler] (go (handler))))]
    (go-loop []
      (when-let [msg (<! ev-chan)]
        (<! (handler-wrapper
             ;; Handle event
             #(try
                (log/debug "Handling event:" (pr-str msg))
                (handle-event* router-ctx msg)
                (catch :default ex
                  (js/Raven.captureException ex)
                  (log/error ex "Error handling event:" (pr-str msg))
                  ))))
        (recur)))))
