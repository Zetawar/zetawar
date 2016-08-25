(ns zetawar.components.timbre
  (:require
    [com.stuartsierra.component :as component]
    [taoensso.timbre :as timbre]))

(defrecord Timbre []
  component/Lifecycle
  (start [component]
    (timbre/merge-config!
      {:appenders
       {:console (timbre/console-appender {})}})
    component)
  (stop [component]
    component))

(defn new-timbre
  []
  (map->Timbre {}))
