(ns zetawar.devcards.prototype
  (:require
   [clojure.string :as string]
   [taoensso.timbre :as log]
   [com.stuartsierra.component :as component]
   [datascript.core :as d]
   [devcards.core :as dc :include-macros true]
   [goog.string :as gstring]
   [posh.core :as posh]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :refer [e]]
   [zetawar.events.ui :as events.ui]
   [zetawar.game :as game]
   [zetawar.router :as router]
   [zetawar.subs :as subs]
   [zetawar.system :refer [new-system]]
   [zetawar.util :refer [breakpoint inspect]]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

#_(defcard-rg prototype-faction-changes
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

(defn unit-picker [{:keys [conn ev-chan] :as app}]
  (let [unit-types @(subs/available-unit-types conn)
        cur-faction @(subs/current-faction conn)
        color (name (:faction/color cur-faction))]
    [:> js/ReactBootstrap.Modal {:show @(subs/show-unit-picker? conn)
                                 :on-hide #(router/dispatch ev-chan [::events.ui/hide-unit-picker])}
     [:> js/ReactBootstrap.Modal.Header {:close-button true}
      [:> js/ReactBootstrap.Modal.Title
       "Select a unit to build"]]
     [:> js/ReactBootstrap.Modal.Body
      (into [:div.unit-picker]
            (for [unit-type unit-types]
              (let [image (->> (string/replace (:unit-type/image unit-type)
                                               "COLOR" color)
                               (str "/images/game/"))
                    media-class (if (:affordable unit-type)
                                  "media"
                                  "media text-muted")]
                [:div {:class media-class
                       :on-click #(router/dispatch ev-chan [::events.ui/build-unit (:unit-type/id unit-type)])}
                 [:div.media-left.media-middle
                  [:img {:src image}]]
                 [:div.media-body
                  [:h4.media-heading
                   (:unit-type/name unit-type)]
                  (str "Cost: " (:unit-type/cost unit-type))]])))]]))

(defcard-rg prototype-unit-type-picker
  (let [{:keys [app]} (component/start (new-system))]
    (app/start-new-game! app :sterlings-aruba-multiplayer)
    [unit-picker app]))
