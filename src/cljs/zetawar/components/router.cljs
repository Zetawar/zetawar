(ns zetawar.components.router
  (:require
   [cljs.core.async :as async]
   [com.stuartsierra.component :as component]
   [zetawar.router :as router]))

(defrecord Router [datascript timbre conn ev-chan players]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript]
      (router/start {:ev-chan ev-chan :conn conn}))
    (assoc component :conn (:conn datascript)))
  (stop [component]
    (async/close! ev-chan)
    (assoc component :datascript nil :timbre nil :conn nil :ev-chan nil :players nil)))

(defn new-router
  ([]
   (new-router (async/chan (async/dropping-buffer 10))))
  ([ev-chan]
   (new-router ev-chan (atom {})))
  ([ev-chan players]
   (component/using (map->Router {:ev-chan ev-chan :players players})
                    [:datascript :timbre])))

