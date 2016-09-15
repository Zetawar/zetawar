(ns zetawar.components.router
  (:require
   [cljs.core.async :as async]
   [com.stuartsierra.component :as component]
   [zetawar.router :as router]))

(defrecord Router [ai datascript timbre ev-chan conn]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript]
      (router/start {:conn conn :ev-chan ev-chan}))
    (assoc component :conn (:conn datascript)))
  (stop [component]
    (async/close! ev-chan)
    (assoc component :ai nil :datascript nil :timbre nil :ev-chan nil :conn nil)))

(defn new-router
  ([]
   (new-router (async/chan (async/dropping-buffer 10))))
  ([ev-chan]
   (component/using (map->Router {:ev-chan ev-chan})
                    [:ai :datascript :timbre])))
