(ns zetawar.benchmarks
  (:require
    [clojure.string :as string]
    [datascript.core :as d]
    [zetawar.data :as data]
    [zetawar.db :refer [qe]]
    [zetawar.game :as game]
    [zetawar.hex :as hex]
    [reagent.core :as r]))

(defn setup-conn []
  (let [conn (d/create-conn data/schema)]
    (game/load-specs! conn)
    (let [game-id (game/setup-game! conn "Sterling's Aruba")]
      (d/transact! conn [{:db/id -1
                          :app/game [:game/id game-id]}]))
    conn))

(defn run-benchmarks []
  (let [suite (js/Benchmark.Suite.)
        db @(setup-conn)
        game (qe '[:find ?g
                   :where
                   [_ :app/game ?g]]
                 db)
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
