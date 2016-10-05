(ns zetawar.app
  (:require
   [cognitect.transit :as transit]
   [datascript.core :as d]
   [goog.crypt.base64 :as base64]
   [lzw]
   [zetawar.db :refer [e find-by qe qes]]
   [zetawar.data :as data]
   [zetawar.game :as game]
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

(defn start-new-game! [conn scenario-id]
  (if-let [game (current-game @conn)]
    (d/transact! conn [[:db.fn/retractEntity (e game)]])
    (game/load-specs! conn))
  (let [scenario-def (data/scenario-definitions scenario-id)
        game-id (game/load-scenario! conn data/map-definitions scenario-def)
        app-eid (or (some-> (root @conn) e) -101)]
    (d/transact! conn [{:db/id app-eid
                        :app/game [:game/id game-id]}])))

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

(defn load-encoded-game-state! [conn encoded-game-state]
  (game/load-specs! conn)
  (let [game-state (decode-game-state encoded-game-state)
        game-id (game/load-game-state! conn
                                       data/map-definitions
                                       data/scenario-definitions
                                       game-state)]
    (d/transact! conn [{:db/id -1
                        :app/game [:game/id game-id]}])))

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
