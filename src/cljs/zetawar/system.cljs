(ns zetawar.system
  (:require
   [integrant.core :as ig]
   [zetawar.db :as db]
   [zetawar.system.datascript]
   [zetawar.system.game-views]
   [zetawar.system.game]
   [zetawar.system.logger]
   [zetawar.system.players]
   [zetawar.system.router]))

(def game-config
  {:zetawar.system/logger     {}
   :zetawar.system/datascript {:schema     db/schema}
   :zetawar.system/players    {}
   :zetawar.system/router     {:datascript (ig/ref :zetawar.system/datascript)
                               :players    (ig/ref :zetawar.system/players)}
   :zetawar.system/game       {:datascript (ig/ref :zetawar.system/datascript)
                               :router     (ig/ref :zetawar.system/router)
                               :players    (ig/ref :zetawar.system/players)}
   :zetawar.system/game-views {:datascript (ig/ref :zetawar.system/datascript)
                               :router     (ig/ref :zetawar.system/router)
                               :locale     :en}})
