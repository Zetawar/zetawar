(ns zetawar.system
  (:require
    [com.stuartsierra.component :as component]
    [zetawar.components.app :refer [new-app]]
    [zetawar.components.datascript :refer [new-datascript]]
    [zetawar.components.posh :refer [new-posh]]
    [zetawar.data :as data]))

(defn new-system
  []
  (component/system-map
    :datascript (new-datascript data/schema)
    :posh       (new-posh)
    :app        (new-app)))
