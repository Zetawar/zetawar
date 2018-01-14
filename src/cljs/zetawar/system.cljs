(ns zetawar.system
  (:require
   [integrant.core :as ig]
   [zetawar.db :as db]))

;; TODO: rename to browser-cfg
(def game-config
  {:zetawar.system/datascript {:schema     db/schema}
   :zetawar.system/reagent    {}
   :zetawar.system/players    {}
   :zetawar.system/router     {:datascript (ig/ref :zetawar.system/datascript)
                               :renderer   (ig/ref :zetawar.system/reagent)
                               :players    (ig/ref :zetawar.system/players)}
   :zetawar.system/game       {:datascript (ig/ref :zetawar.system/datascript)
                               :router     (ig/ref :zetawar.system/router)
                               :players    (ig/ref :zetawar.system/players)}
   :zetawar.system/game-views {:datascript (ig/ref :zetawar.system/datascript)
                               :router     (ig/ref :zetawar.system/router)
                               :locale     :en}})

;; TODO: add cli-cfg
