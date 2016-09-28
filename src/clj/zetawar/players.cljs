(ns zetawar.players
  (:require
   [zetawar.players.embedded-ai :as players.embedded-ai]))

;; TODO: could this be a multimethod or would it result in a circular dep?
(defn new-player [player-ctx player-type faction-color]
  (case player-type
    :zetawar.players/embedded-ai
    (players.embedded-ai/new-player player-ctx faction-color)))

(defn notify [{:as router-ctx :keys [notify-chan]} faction-color msg]
  (put! notify-chan [faction-color msg]))

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
