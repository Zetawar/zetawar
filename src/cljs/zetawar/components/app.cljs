(ns zetawar.components.app
  (:require
    [com.stuartsierra.component :as component]
    [posh.core :as posh]))

(defrecord App [datascript conn]
  component/Lifecycle
  (start [component]
    (assoc component :conn (:conn datascript)))
  (stop [component]
    (assoc component :datascript nil :conn nil)))

(defn new-app
  []
  (component/using (map->App {})
    [:datascript :timbre]))
