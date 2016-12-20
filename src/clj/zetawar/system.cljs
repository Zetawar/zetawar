(ns zetawar.system
  (:require
   [com.stuartsierra.component :as component]
   [zetawar.components.app :refer [new-app]]
   [zetawar.components.datascript :refer [new-datascript]]
   [zetawar.components.posh :refer [new-posh]]
   [zetawar.components.router :refer [new-router]]
   [zetawar.components.timbre :refer [new-timbre]]
   [zetawar.db :as db]))

;; Integrant
;; - zetawar.components/logger
;;     returns nil
;; - zetawar.components/datascript {:schema ...}
;;     returns <DataScript>
;; - zetawar.components/router {:datascript ...}
;;     returns {:ev-chan ... :notify-chan ... :notify-pub ...}
;; - zetawar.components/players {:router ...}
;;     returns <atom {...}>
;; - zetawar.components/views {:datascript ... :router ... :players ...}

(defn new-system
  []
  (component/system-map
   :timbre     (new-timbre)
   :datascript (new-datascript db/schema)
   :posh       (new-posh)
   :router     (new-router)
   :app        (new-app)))
