(ns zetawar.system.game
  (:require
   [integrant.core :as ig]
   [zetawar.logging :as log]
   [zetawar.router :as router]))

;; TODO: setup initial game + load game state (move from core)
(defmethod ig/init-key :zetawar.system/game [_ opts]
  (let [{:keys [datascript players router]} opts
        {:keys [conn]} datascript
        {:keys [ev-chan notify-pub]} router]
    {:conn conn
     :players players
     :ev-chan ev-chan
     :notify-pub notify-pub}))
