(ns zetawar.app
  (:require
   [cognitect.transit :as transit]
   [datascript.core :as d]
   [goog.crypt.base64 :as base64]
   [lzw]
   [taoensso.timbre :as log]
   [zetawar.data :as data]
   [zetawar.db :refer [e find-by qe qes qess]]
   [zetawar.game :as game]
   [zetawar.players :as players]
   [zetawar.util :refer [breakpoint inspect]]))

(defn root [db]
  (qe '[:find ?a
        :where
        [?a :app/game]]
      db))

(defn current-game [db]
  (qe '[:find ?g
        :where
        [_ :app/game ?g]]
      db))

(defn create-players! [{:as app-ctx :keys [conn players]}]
  (let [factions (qess '[:find ?f
                         :where
                         [_  :app/game ?g]
                         [?g :game/factions ?f]]
                       @conn)]
    (doseq [{:keys [faction/ai faction/color]} factions]
      (let [player-type (if ai ::players/reference-ai ::players/human)
            player (players/new-player app-ctx player-type color)]
        (players/start player)
        (swap! players assoc color player)))))

(defn start-new-game! [{:as app-ctx :keys [conn players]} scenario-id]
  (let [game (current-game @conn)]
    (when-not game
      (game/load-specs! conn))
    (let [scenario-def (data/scenario-definitions scenario-id)
          game-id (game/load-scenario! conn data/map-definitions scenario-def)
          app-eid (or (some-> (root @conn) e) -101)]
      (d/transact! conn (cond-> [{:db/id app-eid
                                  :app/game [:game/id game-id]}]
                          game (conj [:db.fn/retractEntity (e game)])))
      (if players
        (create-players! app-ctx)
        (log/warnf "Skipping player creation for tests")))))

(defn encode-game-state [game-state]
  (let [writer (transit/writer :json)]
    (-> (transit/write writer game-state)
        js/lzwEncode
        js/encodeURIComponent
        js/unescape
        (base64/encodeString true))))

(defn decode-game-state [encoded-game-state]
  (let [reader (transit/reader :json)
        transit-game-state (-> encoded-game-state
                               (base64/decodeString true)
                               js/escape
                               js/decodeURIComponent
                               js/lzwDecode)]
    (transit/read reader transit-game-state)))

(defn load-encoded-game-state! [{:as app-ctx :keys [conn players]} encoded-game-state]
  (game/load-specs! conn)
  (let [game-state (decode-game-state encoded-game-state)
        game-id (game/load-game-state! conn
                                       data/map-definitions
                                       data/scenario-definitions
                                       game-state)]
    (d/transact! conn [{:db/id -1
                        :app/game [:game/id game-id]}])

    (if players
      (create-players! app-ctx)
      (log/warnf "Skipping player creation for tests"))))

;; TODO: put URL in paste buffer
(defn set-url-game-state! [db]
  (let [encoded-game-state (->> (current-game db)
                                (game/get-game-state db)
                                encode-game-state)]
    (set! js/window.location.hash encoded-game-state)))

(defn current-game-id [db]
  (:game/id (current-game db)))

(defn selected-hex [db]
  (first (d/q '[:find ?q ?r
                :where
                [?a :app/selected-q ?q]
                [?a :app/selected-r ?r]]
              db)))

(defn targeted-hex [db]
  (first (d/q '[:find ?q ?r
                :where
                [?a :app/targeted-q ?q]
                [?a :app/targeted-r ?r]]
              db)))
