(ns zetawar.system.players
  (:require
   [integrant.core :as ig]
   [zetawar.players :as players]))

(defmethod ig/init-key :zetawar.system/players [_ opts]
  (atom {}))

;; TODO: re-enable once game setup is part of Integrant system
#_(defmethod ig/halt-key! :zetawar.system/players [_ players]
    (doseq [player players]
      (players/stop player)))
