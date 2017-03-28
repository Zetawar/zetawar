(ns zetawar.devcards.maps-and-scenarios
  (:require
   [devcards.core :as dc :include-macros true]
   [integrant.core :as ig]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.system :as system]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(defn init-scenario [scenario-id]
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)]
    (app/start-new-game! game-cfg scenario-id)
    system))

(defcard-rg sterlings-aruba-multiplayer-card
  (let [views-cfg (-> (init-scenario :sterlings-aruba-multiplayer)
                      :zetawar.system/game-views)]
    [views/board views-cfg]))

(defcard-rg city-sprawl-multiplayer-card
  (let [views-cfg (-> (init-scenario :city-sprawl-multiplayer)
                      :zetawar.system/game-views)]
    [views/board views-cfg]))
