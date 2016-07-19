(ns zetawar.core
  (:require
    [com.stuartsierra.component :as component]
    [datascript.core :as d]
    [posh.core :as posh]
    [reagent.core :as r]
    [zetawar.app :as app]
    [zetawar.benchmarks :as benchmarks]
    [zetawar.game :as game]
    [zetawar.system :refer [new-system]]
    [zetawar.util :refer [spy]]
    [zetawar.views :as views]
    [zetawar.views.common :refer [navbar]]))

(defonce system (component/start (new-system)))

(defn ^:export run []
  (enable-console-print!)

  (let [body-id (-> js/document
                    (.getElementsByTagName "body")
                    (aget 0)
                    .-id)]
    (case body-id
      "app"
      (let [target (.getElementById js/document "main")
            {:keys [app]} system]
        (r/render-component
          [views/app-root app]
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
  (let [conn (get-in system [:datascript :conn])
        encoded-game-state (some-> js/window.location.hash
                                   not-empty
                                   (subs 1))]
    (if encoded-game-state
      (app/load-encoded-game-state! conn encoded-game-state)
      (app/start-new-game! conn :sterlings-aruba-multiplayer))
    (set! (.-onload js/window) run)))
