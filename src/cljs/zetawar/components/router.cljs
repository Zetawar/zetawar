(ns zetawar.components.router
  (:require
    [cljs.core.async :refer [chan close!]]
    [com.stuartsierra.component :as component]
    [zetawar.router :as router]))

(defrecord Router [datascript timbre ev-chan conn]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript]
      (router/start {:conn conn :ev-chan ev-chan}))
    (assoc component :conn (:conn datascript)))
  (stop [component]
    (close! ev-chan)
    (assoc component :datascript nil :timbre nil :ev-chan nil :conn nil)))

(defn new-router
  ([]
   (new-router (chan 1)))
  ([ev-chan]
   (component/using (map->Router {:ev-chan ev-chan})
     [:datascript :timbre])))
