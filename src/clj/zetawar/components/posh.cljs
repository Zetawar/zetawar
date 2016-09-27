(ns zetawar.components.posh
  (:require
    [com.stuartsierra.component :as component]
    [datascript.core :as d]
    [posh.core :as posh]
    [reagent.core :as r]))

(defrecord Posh [datascript]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript]
      (posh/posh! conn)
      (d/listen! conn :posh-flusher #(r/flush)))
    component)
  (stop [component]
    (when-let [{:keys [conn]} datascript]
      (d/unlisten! conn :posh-flusher))
    component))

(defn new-posh
  []
  (component/using (map->Posh {})
    [:datascript]))
