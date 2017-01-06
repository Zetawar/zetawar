(ns zetawar.system.datascript
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]))

(defmethod ig/init-key :zetawar.system/datascript [_ opts]
  (let [{:keys [schema]} opts]
    {:schema schema
     :conn (d/create-conn schema)}))

;; TODO: implement resume-key
