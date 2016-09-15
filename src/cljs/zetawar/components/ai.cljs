(ns zetawar.components.ai
  (:require
   [cljs.core.async :as async]
   [com.stuartsierra.component :as component]
   [zetawar.router :as router]))

(defrecord AI [bots]
  component/Lifecycle
  (start [component]
    (assoc component :bots (atom {})))
  (stop [component]
    (doseq [[_ {:keys [req-chan notify-chan]}] bots]
      (async/close! req-chan)
      (async/close! notify-chan))
    (assoc component :bots nil)))

(defn new-ai
  []
  (component/using (map->AI {})
                   [:timbre]))
