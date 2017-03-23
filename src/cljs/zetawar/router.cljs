(ns zetawar.router
  (:require
   [cljs.core.async :refer [chan close! put! sliding-buffer]]
   [cljsjs.raven]
   [datascript.core :as d]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [zetawar.players :as players])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defmulti handle-event (fn [ev-ctx [ev-type & _]] ev-type))

(defmethod handle-event :default
  [_ msg]
  (let [log-msg (str "Unhandled event: " (pr-str msg))]
    (js/Raven.captureMessage log-msg)
    (log/warn log-msg))
  nil)

;; TODO: add spec for dispatch
(defn dispatch [ch msg]
  (if msg
    (do
      (log/debugf "Dispatching event: %s" (pr-str msg))
      (put! ch msg))
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

(defn start [{:as router-ctx :keys [ev-chan]}]
  (let [start-ts (atom -1)
        last-render-ts (atom 0)
        render-chan (chan (sliding-buffer 1))]
    (go-loop [msg (<! ev-chan)]
      (when (< @start-ts @last-render-ts)
        (let [current-ts (.getTime (js/Date.))]
          (log/tracef "Setting start-ts to %d" current-ts)
          (reset! start-ts current-ts)))
      (r/next-tick #(let [current-ts (.getTime (js/Date.))]
                      (log/trace "Rendering...")
                      (put! render-chan :rendered)
                      (when (> current-ts @last-render-ts)
                        (log/tracef "Setting last-render-ts to %d" current-ts)
                        (reset! last-render-ts current-ts))))
      (when msg
        (try
          (log/debugf "Handling event: %s" (pr-str msg))
          ;; TODO: validate event
          ;; TODO: validate handler return value
          (handle-event* router-ctx msg)
          (catch :default ex
            (js/Raven.captureException ex)
            (log/errorf ex "Error handling event: %s" (pr-str msg))))
        (when (< @last-render-ts @start-ts)
          (let [ts-diff (- (.getTime (js/Date.)) @start-ts)]
            (log/tracef "Milliseconds since render timer started: %d" ts-diff)
            ;; TODO: make ts-diff threshold an argument to start
            (when (> ts-diff 200)
              (log/trace "Blocking till next render...")
              (<! render-chan)
              (log/trace "Render completed; unblocking"))))
        (recur (<! ev-chan))))))
