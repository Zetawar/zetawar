(ns zetawar.router.reagent
  (:require
   [cljs.core.async :refer [<! >! chan offer!]]
   [reagent.core :as r]
   [zetawar.logging :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go]]))

;; TODO: consider alternative names (event-wrapper-fn?)
(defn handler-wrapper-fn [{:as router-ctx :keys [max-render-interval]} event-handler]
  (let [timer-start (atom -1)
        last-render (atom 0)
        render-queued? (atom false)
        render-chan (chan 1)]
    (fn reagent-handler-wrapper [event-handler]
      (go
        ;; Reset render timer if a render just occurred
        (when (< @timer-start @last-render)
          (let [now (.getTime (js/Date.))]
            (reset! timer-start now)
            (log/trace "@timer-start:" @timer-start)))

        ;; Queue notification of render
        (when-not @render-queued?
          (r/next-tick #(let [now (.getTime (js/Date.))]
                          (log/trace "Rendering...")
                          (offer! render-chan :rendered)
                          (when (> now @last-render)
                            (reset! last-render now)
                            (log/trace "@last-render:" @last-render)
                            (reset! render-queued? false)))))

        (event-handler)

        ;; Block till render if max-render-interval has been exceeded
        (let [since-last-render (- (.getTime (js/Date.)) @timer-start)]
          (log/trace "since-last-render:" since-last-render)
          (when (> since-last-render max-render-interval)
            (log/trace "Blocking till next render...")
            (<! render-chan)
            (log/trace "Render completed; unblocking")))))))
