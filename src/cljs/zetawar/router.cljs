(ns zetawar.router
  (:require
   [cljs.core.async :refer [<! >! chan offer!]]
   [cljsjs.raven]
   [datascript.core :as d]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [zetawar.players :as players])
  (:require-macros
   [cljs.core.async.macros :refer [go-loop]]))

;; TODO: add specs for handle-event
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

(defn start [{:as router-ctx :keys [ev-chan max-render-interval]}]
  (let [timer-start (atom -1)
        last-render (atom 0)
        render-queued? (atom false)
        render-chan (chan 1)]
    (go-loop []
      (when-let [msg (<! ev-chan)]
        ;; Reset render timer if a render just occurred
        (when (< @timer-start @last-render)
          (let [now (.getTime (js/Date.))]
            (log/tracef "Setting timer-start to %d" now)
            (reset! timer-start now)))

        ;; Queue notification of render
        (when-not @render-queued?
          (r/next-tick #(let [now (.getTime (js/Date.))]
                          (log/trace "Rendering...")
                          (offer! render-chan :rendered)
                          (when (> now @last-render)
                            (log/tracef "Setting last-render to %d" now)
                            (reset! last-render now)
                            (reset! render-queued? false)))))

        ;; Handle event
        (try
          (log/debugf "Handling event: %s" (pr-str msg))
          (handle-event* router-ctx msg)
          (catch :default ex
            (js/Raven.captureException ex)
            (log/errorf ex "Error handling event: %s" (pr-str msg))))

        ;; Block till render if max-render-interval has been exceeded
        (let [since-last-render (- (.getTime (js/Date.)) @timer-start)]
          (log/tracef "Milliseconds since render timer started: %d" since-last-render)
          (when (> since-last-render max-render-interval)
            (log/trace "Blocking till next render...")
            (<! render-chan)
            (log/trace "Render completed; unblocking")))

        (recur)))))
