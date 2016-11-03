(ns zetawar.devcards.scenarios
  (:require
   [com.stuartsierra.component :as component]
   [datascript.core :as d]
   [devcards.core :as dc :include-macros true]
   [posh.core :as posh]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.system :as system]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(defcard-rg sterlings-aruba-multiplayer-card
  (let [{:keys [app]} (component/start (system/new-system))]
    (app/start-new-game! app :sterlings-aruba-multiplayer)
    [views/board app]))
