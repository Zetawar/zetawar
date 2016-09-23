(ns zetawar.players)

(defn new-player []
  ;; returns map with req and event chans wired up to default handlers
  )

(defn notify [{:keys [conn ev-chan players]} faction-color msg]
  ;; create bot if it doesn't exist
  ;; queue message on notify-chan
  )

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
