(ns zetawar.core
  (:require
   [cljsjs.raven]
   [integrant.core :as ig]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.benchmarks :as benchmarks]
   [zetawar.db :as db]
   [zetawar.events.game]
   [zetawar.events.player]
   [zetawar.events.ui]
   [zetawar.game :as game]
   [zetawar.js.game]
   [zetawar.js.hex]
   [zetawar.logging :as log]
   [zetawar.players.ai.custom-js]
   [zetawar.players.ai.custom]
   [zetawar.players.ai.reference]
   [zetawar.players.human]
   [zetawar.router.reagent]
   [zetawar.serialization :as serialization]
   [zetawar.site :as site]
   [zetawar.system :as system]
   [zetawar.system.datascript]
   [zetawar.system.game-views]
   [zetawar.system.game]
   [zetawar.system.players]
   [zetawar.system.reagent]
   [zetawar.system.router]
   [zetawar.util :refer [breakpoint inspect]]
   [zetawar.views :as views]
   [zetawar.views.common :refer [navbar]]))

(defonce system (atom nil))

(defn run []
  (let [body-id (-> js/document
                    (.getElementsByTagName "body")
                    (aget 0)
                    .-id)]
    (case body-id
      "app"
      (let [target (.getElementById js/document "main")
            views-cfg (:zetawar.system/game-views @system)]
        (r/render-component
         [views/app-root views-cfg]
         target))

      "site"
      (let [target (.getElementById js/document "navbar-wrapper")
            active-navbar-title (.getAttribute target "data-active-title")]
        (r/render-component
         [navbar active-navbar-title]
         target))

      "benchmarks"
      (benchmarks/run-benchmarks)

      ;; Default
      nil)))

;; TODO: don't start game when viewing non-game site
;; TODO: don't start game when running benchmarks
(defn ^:export init []
  (when-not (site/viewing-devcards?)
    (reset! system (ig/init system/game-config))
    (let [game-cfg (:zetawar.system/game @system)
          encoded-game-state (some-> js/window.location.hash
                                     not-empty
                                     (subs 1))]
      (if encoded-game-state
        (serialization/load-encoded-game-state! game-cfg encoded-game-state)
        (app/start-new-game! game-cfg :sterlings-aruba-multiplayer))
      (set! (.-onload js/window) run))))

(defn ^:export reload []
  (when-not (site/viewing-devcards?)
    (ig/suspend! @system)
    (reset! system (ig/resume system/game-config @system)))
  (run))
