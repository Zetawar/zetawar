(ns zetawar.components.posh
  (:require
   [com.stuartsierra.component :as component]
   [datascript.core :as d]
   [posh.reagent :as posh]
   [reagent.core :as r]))

(defrecord Posh [datascript]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript]
      (posh/posh! conn))
    component)
  (stop [component]
    component))

(defn new-posh
  []
  (component/using (map->Posh {})
                   [:datascript]))
