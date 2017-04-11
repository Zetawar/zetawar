(ns zetawar.system.datascript
  (:require
   [datascript.core :as d]
   [integrant.core :as ig]))

(defmethod ig/init-key :zetawar.system/datascript [_ opts]
  (let [{:keys [schema]} opts]
    {:schema schema
     :conn (d/create-conn schema)}))

(defmethod ig/resume-key :zetawar.system/datascript [_ opts old-opts old-impl]
  (let [{:keys [schema]} opts
        old-schema (:schema old-opts)]
    (if (= schema old-schema)
      old-impl
      {:schema schema
       :conn (d/create-conn schema)})))
