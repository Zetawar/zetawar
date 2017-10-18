(ns zetawar.system.reagent
  (:require
   [integrant.core :as ig]
   [zetawar.router.reagent]
   [taoensso.timbre :as log]))

(defmethod ig/init-key :zetawar.system/reagent [_ opts]
  {:handler-wrapper-fn zetawar.router.reagent/handler-wrapper-fn})
