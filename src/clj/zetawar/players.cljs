(ns zetawar.players
  (:require
   [cljs.core.async :as async]
   [taoensso.timbre :as log]
   [zetawar.data :as data]
   [zetawar.game :as game]))

(defprotocol Player
  (start [player])
  (stop [player]))

(defmulti new-player (fn [player-ctx player-type faction-color] player-type))

(defn notify [notify-chan msg]
  (log/debugf "Notifying player: %s" (pr-str msg))
  (async/put! notify-chan msg))

(defn load-player-game-state! [conn game-state]
  (game/load-specs! conn)
  (game/load-game-state! conn
                         data/map-definitions
                         data/scenario-definitions
                         game-state))

;; requests
;; - get-state
;; - move
;; - attack
;; - capture
;; - build
;; - end-turn

;; events
;; - start-turn
;; - game-state

;; events structure
;; - event-type

;; request response notification
;; - request-id
;; - response or error
