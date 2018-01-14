(ns zetawar.benchmarks
  (:require
   [clojure.string :as string]
   [datascript.core :as d]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :as db :refer [qe]]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.logging :as log]))

(defn setup-conn []
  (let [conn (d/create-conn db/schema)]
    (app/start-new-game! {:conn conn} :sterlings-aruba-multiplayer)
    conn))

(defn run-benchmarks []
  (let [suite (js/Benchmark.Suite.)
        db @(setup-conn)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (-> suite
        (.add "valid-moves"
              (fn []
                (game/valid-moves db game unit)))
        (.on "complete"
             (fn []
               (this-as this
                 (js/console.log (str "valid-moves: " (aget this 0))))))
        (.run))))
