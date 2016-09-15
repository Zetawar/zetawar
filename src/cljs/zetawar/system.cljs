(ns zetawar.system
  (:require
   [com.stuartsierra.component :as component]
   [zetawar.components.ai :refer [new-ai]]
   [zetawar.components.app :refer [new-app]]
   [zetawar.components.datascript :refer [new-datascript]]
   [zetawar.components.posh :refer [new-posh]]
   [zetawar.components.router :refer [new-router]]
   [zetawar.components.timbre :refer [new-timbre]]
   [zetawar.db :as db]))

(defn new-system
  []
  (component/system-map
   :ai         (new-ai)
   :timbre     (new-timbre)
   :datascript (new-datascript db/schema)
   :posh       (new-posh)
   :router     (new-router)
   :app        (new-app)))
