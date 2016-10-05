(ns zetawar.players
  (:require
   [cljs.core.async :as async]
   [taoensso.timbre :as log]))

(defprotocol Player
  (start [player])
  (stop [player]))

(defmulti new-player (fn [player-ctx player-type faction-color] player-type))

(defn notify [notify-chan msg]
  (log/debugf "Notifying player: %s" (pr-str msg))
  (async/put! notify-chan msg))

;; TODO: handle-request is probably unecessary, can be part of regular event processing

;; handler-request - implemented by zetawar "server"
(defn handle-request [{:keys [conn ev-chan players]} faction-color msg]
  )

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
