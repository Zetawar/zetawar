(ns zetawar.devcards.prototype
  (:require
    [clojure.string :as string]
    [com.stuartsierra.component :as component]
    [datascript.core :as d]
    [devcards.core :as dc :include-macros true]
    [goog.string :as gstring]
    [posh.core :as posh]
    [reagent.core :as r]
    [zetawar.app :as app]
    [zetawar.data :as data]
    [zetawar.db :refer [e]]
    [zetawar.game :as game]
    [zetawar.events :as handlers]
    [zetawar.subs :as subs]
    [zetawar.system :refer [new-system]]
    [zetawar.util :refer [spy]]
    [zetawar.views :as views])
  (:require-macros
    [devcards.core :refer [defcard defcard-rg]]))

(defcard-rg prototype-faction-changes
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    [:div.row
     [:div.col-md-2
      [views/faction-list app]
      [views/faction-actions app]]
     [:div.col-md-10
      [views/faction-status app]
      [views/board app]]]))

(defn unit-type-picker [{:keys [conn] :as app}]
  (let [unit-types @(subs/buildable-unit-types conn)
        cur-faction @(subs/current-faction conn)
        color (name (:faction/color cur-faction))]
    (into [:div.unit-picker]
          (for [unit-type unit-types]
            (let [image (->> (string/replace (:unit-type/image unit-type)
                                             "COLOR" color)
                             (str "/images/game/"))]
              [:div.media
               [:div.media-left.media-middle
                [:img {:src image}]]
               [:div.media-body
                [:h4.media-heading (:unit-type/name unit-type)]
                (str "Cost: " (:unit-type/cost unit-type))]])))))

(defcard-rg prototype-unit-type-picker
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    [unit-type-picker app]))
