(ns zetawar.devcards.maps
  (:require
    [com.stuartsierra.component :as component]
    [datascript.core :as d]
    [devcards.core :as dc :include-macros true]
    [posh.core :as posh]
    [reagent.core :as r]
    [zetawar.app :as app]
    [zetawar.data :as data]
    [zetawar.game :as game]
    [zetawar.system :refer [new-system]]
    [zetawar.views :as views])
  (:require-macros
    [devcards.core :refer [defcard defcard-rg]]))

(defcard-rg sterlings-aruba-card
  (let [system (component/start (new-system))
        {:keys [app]} system
        conn (get-in system [:datascript :conn])]
    (app/start-new-game! conn :sterlings-aruba-multiplayer)
    [views/board app]))
