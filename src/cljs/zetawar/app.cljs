(ns zetawar.app
  (:require
   [cognitect.transit :as transit]
   [datascript.core :as d]
   [zetawar.data :as data]
   [zetawar.db :refer [e find-by qe qes qess]]
   [zetawar.game :as game]
   [zetawar.logging :as log]
   [zetawar.players :as players]
   [zetawar.util :as util :refer [breakpoint inspect]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; DB Accessors

(defn root [db]
  (find-by db :app/game))

(defn current-game [db]
  (:app/game (root db)))

(defn current-game-id [db]
  (:game/id (current-game db)))

(defn selected-hex [db]
  (let [{:keys [app/selected-q app/selected-r]} (root db)]
    [selected-q selected-r]))

(defn targeted-hex [db]
  (let [{:keys [app/targeted-q app/targeted-r]} (root db)]
    [targeted-q targeted-r]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game setup

(defn create-players! [{:as app-ctx :keys [conn players]}]
  (let [factions (qess '[:find ?f
                         :where
                         [_  :app/game ?g]
                         [?g :game/factions ?f]]
                       @conn)]
    ;; TODO: cleanup (relocate?) player stopping
    ;; Stop existing players (when starting new games)
    (when players
      (doseq [[_ player] @players]
        (when player
          (players/stop player))))
    (doseq [{:as faction :keys [faction/ai faction/color]} factions]
      (let [player-type (if ai ::players/reference-ai ::players/human)
            player (players/new-player app-ctx player-type color)]
        (d/transact! conn [{:db/id (e faction)
                            :faction/player-type player-type}])
        (players/start player)
        (swap! players assoc color player)))))

(defn start-new-game!
  ([{:as app-ctx :keys [conn players]} scenario-id]
   (start-new-game! app-ctx data/rulesets data/maps data/scenarios scenario-id))
  ([{:as app-ctx :keys [conn players]} rulesets map-defs scenario-defs scenario-id]
   (let [game (current-game @conn)]
     (when game
       (d/transact conn [[:db.fn/retractEntity (e game)]]))
     (let [scenario-def (scenario-defs scenario-id)
           game-id (game/load-scenario! conn rulesets map-defs scenario-def)
           db @conn
           app-eid (or (some-> (root db) e) -101)
           game (game/game-by-id db game-id)
           turn-stepping (= (game/faction-count db game)
                            (game/ai-faction-count db game))]
       (d/transact! conn [{:db/id app-eid
                           :app/game [:game/id game-id]
                           :app/hide-win-message false
                           :app/ai-turn-stepping turn-stepping}])
       ;; Skip player creation for tests
       (when players
         (create-players! app-ctx))))))

(defn load-game-state!
  ([{:as app-ctx :keys [conn players]} game-state]
   (load-game-state! app-ctx data/rulesets data/maps data/scenarios game-state))
  ([{:as app-ctx :keys [conn players]} rulesets map-defs scenario-defs game-state]
   (let [game-id (game/load-game-state! conn
                                        rulesets
                                        map-defs
                                        scenario-defs
                                        game-state)
         db @conn
         game (game/game-by-id db game-id)
         turn-stepping (= (game/faction-count db game)
                          (game/ai-faction-count db game))]
     (d/transact! conn [{:db/id -1
                         :app/game [:game/id game-id]
                         :app/ai-turn-stepping turn-stepping}])
     ;; Skip player creation for tests
     (when players
       (create-players! app-ctx)))))
