(ns zetawar.test-helper
  (:require
    [datascript.core :as d]
    [posh.core :as posh]
    [reagent.core :as r]
    [zetawar.app :as app]
    [zetawar.db :as db]
    [zetawar.game :as game]))

(defn create-conn [scenario-id]
  (let [conn (d/create-conn db/schema)]
    (posh/posh! conn)
    (app/start-new-game! conn scenario-id)
    conn))

(defn create-aruba-conn []
  (create-conn :sterlings-aruba-multiplayer))
