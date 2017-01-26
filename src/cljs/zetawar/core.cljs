(ns zetawar.core
  (:require
   [integrant.core :as ig]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.benchmarks :as benchmarks]
   [zetawar.events.game]
   [zetawar.events.player]
   [zetawar.events.ui]
   [zetawar.game :as game]
   [zetawar.js.game]
   [zetawar.js.hex]
   [zetawar.players.ai.custom-js]
   [zetawar.players.ai.custom]
   [zetawar.players.ai.reference]
   [zetawar.players.human]
   [zetawar.site :as site]
   [zetawar.system :as system]
   [zetawar.util :refer [breakpoint inspect]]
   [zetawar.views :as views]
   [zetawar.views.common :refer [navbar]]))

(defonce system (atom nil))

(defn ^:export run []
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

;; TODO: don't start game when running benchmarks
(defn ^:export init []
  (when-not (site/viewing-devcards?)
    (reset! system (ig/init system/game-config))
    (let [game-cfg (:zetawar.system/game @system)
          encoded-game-state (some-> js/window.location.hash
                                     not-empty
                                     (subs 1))]
      (if encoded-game-state
        (app/load-encoded-game-state! game-cfg encoded-game-state)
        (app/start-new-game! game-cfg :sterlings-aruba-multiplayer))
      (set! (.-onload js/window) run))))
