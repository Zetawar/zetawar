(ns zetawar.bots)

(defn new-bot []
  ;; returns map with req and notify chans wired up to default handlers
  )

(defn notify [{:keys [conn ev-chan bots]} faction-color msg]
  ;; create bot if it doesn't exist
  ;; queue message on notify-chan
  )

;; handler-req is implemented by zetawar
(defn handle-req [{:keys [conn ev-chan bots]} faction-color msg]
  )

;; handle-notify is implemented by bots
(defn handle-notify [{:keys [conn ev-chan bots]} faction-color msg]
  )

;; requests
;; - get-state
;; - move
;; - attack
;; - capture
;; - build
;; - end-turn

;; notifications
;; - start-turn
;; - game-state
