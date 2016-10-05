(ns zetawar.players.human
  (:require
   [zetawar.players :as players]))

(defrecord HumanPlayer [player-ctx faction-color]
  players/Player
  (start [player]
    nil)
  (stop [player]
    nil))

(defmethod players/new-player ::players/human
  [{:as player-ctx :keys [notify-pub]} player-type faction-color]
  (HumanPlayer. player-ctx faction-color))
