(ns zetawar.test-helper
  (:require
   [datascript.core :as d]
   [posh.reagent :as posh]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.game :as game]))

(defn create-scenario-conn [scenario-id]
  (let [conn (d/create-conn db/schema)]
    (posh/posh! conn)
    (app/start-new-game! {:conn conn} scenario-id)
    conn))
