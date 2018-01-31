(ns zetawar.cli
  (:require
   [cljs.nodejs :as nodejs]
   [integrant.core :as ig]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.events.game]
   [zetawar.events.player]
   [zetawar.game :as game]
   [zetawar.players.ai.custom]
   [zetawar.players.ai.reference]
   [zetawar.players.human]
   [zetawar.router :as router]
   [zetawar.system.datascript]
   [zetawar.system.game]
   [zetawar.system.players]
   [zetawar.system.router]))

(def cli-game-config
  {:zetawar.system/datascript {:schema     db/schema}
   :zetawar.system/players    {}
   :zetawar.system/router     {:datascript (ig/ref :zetawar.system/datascript)
                               :players    (ig/ref :zetawar.system/players)}
   :zetawar.system/game       {:datascript (ig/ref :zetawar.system/datascript)
                               :router     (ig/ref :zetawar.system/router)
                               :players    (ig/ref :zetawar.system/players)}})
(nodejs/enable-util-print!)

(defn end-turn [system]
  (let [ev-chan (-> system :zetawar.system/router :ev-chan)
        db @(-> system :zetawar.system/datascript :conn)
        game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    (router/dispatch ev-chan [:zetawar.events.game/execute-action
                              {:action/type :action.type/end-turn
                               :action/faction-color cur-faction-color}])))

(.install (js/require "source-map-support"))

;; TODO: switch to cljs require
(def readline (js/require "readline"))

;; TODO: add ability to pass in scenario
(defn ^:export -main [& args]
  (let [system (ig/init cli-game-config)
        ev-chan (-> system :zetawar.system/router :ev-chan)
        conn (-> system :zetawar.system/datascript :conn)
        rl (.createInterface readline #js {"input" js/process.stdin
                                           "output" js/process.stdout
                                           "terminal" false})]
    (.on rl "line" (fn [line] (end-turn system)))
    (println "Hello from the Zetawar game runner!")
    (app/start-new-game! (:zetawar.system/game system) :sterlings-aruba-ai-vs-ai)
    (router/dispatch ev-chan [:zetawar.events.player/send-game-state
                              (->> @conn
                                   app/current-game
                                   game/current-faction-color)])))

(set! *main-cli-fn* -main)
