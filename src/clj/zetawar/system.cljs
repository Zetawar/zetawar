(ns zetawar.system
  (:require
   [com.stuartsierra.component :as component]
   [integrant.core :as ig]
   [zetawar.components.app :refer [new-app]]
   [zetawar.components.datascript :refer [new-datascript]]
   [zetawar.components.posh :refer [new-posh]]
   [zetawar.components.router :refer [new-router]]
   [zetawar.components.timbre :refer [new-timbre]]
   [zetawar.db :as db]
   [zetawar.system.datascript]
   [zetawar.system.logger]
   [zetawar.system.players]
   [zetawar.system.router]
   [zetawar.system.views]))

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

;; TODO: add :zetawar.system/game to setup initial game state
(def config
  {:zetawar.system/logger {}
   :zetawar.system/datascript {:schema db/schema}
   :zetawar.system/players {}
   :zetawar.system/router {:datascript (ig/ref :zetawar.system/datascript)
                           :players (ig/ref :zetawar.system/players)}
   :zetawar.system/views {:datascript (ig/ref :zetawar.system/datascript)
                          :router (ig/ref :zetawar.system/router)
                          :players (ig/ref :zetawar.system/players)
                          :locale :en}})
