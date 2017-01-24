(ns zetawar.players.human
  (:require
   [zetawar.players :as players]))

(defrecord HumanPlayer [faction-color]
  players/Player
  (start [player])
  (stop [player]))

(defmethod players/new-player ::players/human
  [_ _ faction-color]
  (HumanPlayer. faction-color))
