(ns zetawar.game-runner
  (:require
   [cljs.nodejs :as nodejs]
   [integrant.core :as ig]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.system.datascript]
   [zetawar.system.game]
   [zetawar.system.logger]
   [zetawar.system.players]
   [zetawar.system.router]))

(def cli-game-config
  {:zetawar.system/logger     {}
   :zetawar.system/datascript {:schema     db/schema}
   :zetawar.system/players    {}
   :zetawar.system/router     {:datascript (ig/ref :zetawar.system/datascript)
                               :players    (ig/ref :zetawar.system/players)}
   :zetawar.system/game       {:datascript (ig/ref :zetawar.system/datascript)
                               :router     (ig/ref :zetawar.system/router)
                               :players    (ig/ref :zetawar.system/players)}})
(nodejs/enable-util-print!)

(defn -main [& args]
  (println "Hello from the Zetawar game runner!"))

(set! *main-cli-fn* -main)
