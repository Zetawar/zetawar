(ns zetawar.system.logger
  (:require
   [taoensso.timbre :as timbre]
   [integrant.core :as ig]))

(defmethod ig/init-key :zetawar.system/logger [_ opts]
  (timbre/merge-config!
   {:appenders
    {:console (timbre/console-appender {})}}))
