(ns zetawar.game-runner
  (:require
   [cljs.nodejs :as nodejs]
   [integrant.core :as ig]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.events.game]
   [zetawar.events.player]
   [zetawar.players.ai.custom]
   [zetawar.players.ai.reference]
   [zetawar.players.human]
   [zetawar.router :as router]
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
  (let [system (ig/init cli-game-config)]
    (println "Hello from the Zetawar game runner!")
    (app/start-new-game! (:zetawar.system/game system) :sterlings-aruba-multiplayer)
    (router/dispatch (-> system :zetawar.system/router :ev-chan)
                     [:zetawar.events.game/execute-action
                      {:action/type :action.type/end-turn
                       :action/faction-color :blue}])))

(set! *main-cli-fn* -main)
