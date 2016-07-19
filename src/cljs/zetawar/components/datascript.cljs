(ns zetawar.components.datascript
  (:require
    [com.stuartsierra.component :as component]
    [datascript.core :as d]))

(defrecord Datascript [schema conn]
  component/Lifecycle
  (start [component]
    (assoc component :conn (d/create-conn schema)))
  (stop [component]
    (assoc component :schema nil :conn nil)))

(defn new-datascript
  [schema]
  (map->Datascript {:schema schema}))
