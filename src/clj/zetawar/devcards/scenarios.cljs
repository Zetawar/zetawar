(ns zetawar.devcards.scenarios
  (:require
   [datascript.core :as d]
   [devcards.core :as dc :include-macros true]
   [integrant.core :as ig]
   [posh.core :as posh]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.system :as system]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(defcard-rg sterlings-aruba-multiplayer-card
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)]
    (app/start-new-game! game-cfg :sterlings-aruba-multiplayer)
    [views/board views-cfg]))

(defcard-rg city-sprawl-multiplayer-card
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)]
    (app/start-new-game! game-cfg :city-sprawl-multiplayer)
    [views/board views-cfg]))
