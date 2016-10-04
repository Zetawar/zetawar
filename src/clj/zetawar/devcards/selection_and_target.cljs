(ns zetawar.devcards.selection-and-target
  (:require
   [com.stuartsierra.component :as component]
   [datascript.core :as d]
   [devcards.core :as dc :include-macros true]
   [posh.core :as posh]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :refer [e]]
   [zetawar.game :as game]
   [zetawar.subs :as subs]
   [zetawar.system :refer [new-system]]
   [zetawar.util :refer [breakpoint inspect]]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(defcard-rg unit-selected
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    (d/transact! conn [{:db/id (-> @conn app/root e)
                        :app/selected-q 2
                        :app/selected-r 2}])
    [:div.row
     [:div.col-md-2
      [views/faction-list app]
      [views/faction-actions app]]
     [:div.col-md-10
      [views/faction-status app]
      [views/board app]]]))

(defcard-rg moved-unit-selected
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    (let [game (app/current-game @conn)
          unit (game/unit-at @conn game 2 2)]
      (d/transact! conn [{:db/id (-> @conn app/root e)
                          :app/selected-q 2
                          :app/selected-r 2}
                         [:db/add (e unit) :unit/move-count 1]]))
    [:div.row
     [:div.col-md-2
      [views/faction-list app]
      [views/faction-actions app]]
     [:div.col-md-10
      [views/faction-status app]
      [views/board app]]]))

(defcard-rg moved-unit-with-attacks
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    (let [game (app/current-game @conn)
          unit (game/unit-at @conn game 2 2)]
      (d/transact! conn (concat (game/teleport-tx @conn game 2 2 6 8)
                                [{:db/id (-> @conn app/root e)
                                  :app/selected-q 6
                                  :app/selected-r 8}
                                 [:db/add (e unit) :unit/move-count 1]])))
    [:div.row
     [:div.col-md-2
      [views/faction-list app]
      [views/faction-actions app]]
     [:div.col-md-10
      [views/faction-status app]
      [views/board app]]]))

(defcard-rg targeted-enemy
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    (let [game (app/current-game @conn)
          unit (game/unit-at @conn game 2 2)]
      (d/transact! conn (concat (game/teleport-tx @conn game 2 2 6 8)
                                [{:db/id (-> @conn app/root e)
                                  :app/selected-q 6
                                  :app/selected-r 8
                                  :app/targeted-q 7
                                  :app/targeted-r 8}
                                 [:db/add (e unit) :unit/move-count 1]])))
    [:div.row
     [:div.col-md-2
      [views/faction-list app]
      [views/faction-actions app]]
     [:div.col-md-10
      [views/faction-status app]
      [views/board app]]]))

(defcard-rg base-selected
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    (d/transact! conn [{:db/id (-> @conn app/root e)
                        :app/selected-q 1
                        :app/selected-r 2}])
    [:div.row
     [:div.col-md-2
      [views/faction-list app]
      [views/faction-actions app]]
     [:div.col-md-10
      [views/faction-status app]
      [views/board app]]]))
