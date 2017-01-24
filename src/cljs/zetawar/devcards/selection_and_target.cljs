(ns zetawar.devcards.selection-and-target
  (:require
   [datascript.core :as d]
   [devcards.core :as dc :include-macros true]
   [integrant.core :as ig]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :refer [e]]
   [zetawar.game :as game]
   [zetawar.subs :as subs]
   [zetawar.system :as system]
   [zetawar.util :refer [breakpoint inspect]]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(defcard-rg unit-selected
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)
        conn (:conn views-cfg)]
    (app/start-new-game! game-cfg :sterlings-aruba-multiplayer)
    (d/transact! conn [{:db/id (-> @conn app/root e)
                        :app/selected-q 2
                        :app/selected-r 2}])
    [:div.row
     [:div.col-md-2
      [views/faction-list views-cfg]
      [views/faction-actions views-cfg]]
     [:div.col-md-10
      [views/faction-status views-cfg]
      [views/board views-cfg]]]))

(defcard-rg moved-unit-selected
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)
        conn (:conn views-cfg)]
    (app/start-new-game! game-cfg :sterlings-aruba-multiplayer)
    (let [game (app/current-game @conn)
          unit (game/unit-at @conn game 2 2)]
      (d/transact! conn [{:db/id (-> @conn app/root e)
                          :app/selected-q 2
                          :app/selected-r 2}
                         [:db/add (e unit) :unit/move-count 1]]))
    [:div.row
     [:div.col-md-2
      [views/faction-list views-cfg]
      [views/faction-actions views-cfg]]
     [:div.col-md-10
      [views/faction-status views-cfg]
      [views/board views-cfg]]]))

(defcard-rg moved-unit-with-attacks
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)
        conn (:conn views-cfg)]
    (app/start-new-game! game-cfg :sterlings-aruba-multiplayer)
    (let [game (app/current-game @conn)
          unit (game/unit-at @conn game 2 2)]
      (d/transact! conn (into (game/teleport-tx @conn game 2 2 6 8)
                              [{:db/id (-> @conn app/root e)
                                :app/selected-q 6
                                :app/selected-r 8}
                               [:db/add (e unit) :unit/move-count 1]])))
    [:div.row
     [:div.col-md-2
      [views/faction-list views-cfg]
      [views/faction-actions views-cfg]]
     [:div.col-md-10
      [views/faction-status views-cfg]
      [views/board views-cfg]]]))

(defcard-rg targeted-enemy
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)
        conn (:conn views-cfg)]
    (app/start-new-game! game-cfg :sterlings-aruba-multiplayer)
    (let [game (app/current-game @conn)
          unit (game/unit-at @conn game 2 2)]
      (d/transact! conn (into (game/teleport-tx @conn game 2 2 6 8)
                              [{:db/id (-> @conn app/root e)
                                :app/selected-q 6
                                :app/selected-r 8
                                :app/targeted-q 7
                                :app/targeted-r 8}
                               [:db/add (e unit) :unit/move-count 1]])))
    [:div.row
     [:div.col-md-2
      [views/faction-list views-cfg]
      [views/faction-actions views-cfg]]
     [:div.col-md-10
      [views/faction-status views-cfg]
      [views/board views-cfg]]]))

(defcard-rg base-selected
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)
        conn (:conn views-cfg)]
    (app/start-new-game! game-cfg :sterlings-aruba-multiplayer)
    (d/transact! conn [{:db/id (-> @conn app/root e)
                        :app/selected-q 1
                        :app/selected-r 2}])
    [:div.row
     [:div.col-md-2
      [views/faction-list views-cfg]
      [views/faction-actions views-cfg]]
     [:div.col-md-10
      [views/faction-status views-cfg]
      [views/board views-cfg]]]))
