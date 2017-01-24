(ns zetawar.devcards.prototype
  (:require
   [clojure.string :as string]
   [datascript.core :as d]
   [devcards.core :as dc :include-macros true]
   [goog.string :as gstring]
   [posh.core :as posh]
   [reagent.core :as r]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :refer [e]]
   [zetawar.events.ui :as events.ui]
   [zetawar.game :as game]
   [zetawar.router :as router]
   [zetawar.subs :as subs]
   [zetawar.system :as system]
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


#_(defcard-rg prototype-unit-type-picker
    (let [{:keys [app]} (component/start (new-system))]
      (app/start-new-game! app :sterlings-aruba-multiplayer)
      [faction-settings app]))
