(ns zetawar.system.players
  (:require
   [integrant.core :as ig]
   [zetawar.players :as players]))

(defmethod ig/init-key :zetawar.system/players [_ opts]
  (atom {}))

(defmethod ig/halt-key! :zetawar.system/players [_ players]
  (doseq [player players]
    (players/stop player)))
