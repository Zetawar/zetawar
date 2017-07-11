(ns zetawar.game-runner
  (:require
   [cljs.nodejs :as nodejs]
   [zetawar.game]))

(nodejs/enable-util-print!)

(defn -main [& args]
  (println "Hello from the Zetawar game runner!"))

(set! *main-cli-fn* -main)
