(ns zetawar.core
  (:require
   [com.stuartsierra.component :as component]
   [datascript.core :as d]
   [posh.core :as posh]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.benchmarks :as benchmarks]
   [zetawar.events.game]
   [zetawar.events.player]
   [zetawar.events.ui]
   [zetawar.game :as game]
   [zetawar.players.human]
   [zetawar.players.reference-ai]
   [zetawar.site :as site]
   [zetawar.system :refer [new-system]]
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
      (let [target (.getElementById js/document "main")]
        (r/render-component
         [views/app-root (:app @system)]
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

(defn ^:export init []
  (when-not (site/viewing-devcards?)
    (reset! system (component/start (new-system)))
    (let [app-ctx (:app @system)
          encoded-game-state (some-> js/window.location.hash
                                     not-empty
                                     (subs 1))]
      (if encoded-game-state
        (app/load-encoded-game-state! app-ctx encoded-game-state)
        (app/start-new-game! app-ctx :sterlings-aruba-multiplayer))
      (set! (.-onload js/window) run))))
