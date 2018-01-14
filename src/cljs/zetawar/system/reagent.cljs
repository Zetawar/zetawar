(ns zetawar.system.reagent
  (:require
   [integrant.core :as ig]
   [zetawar.logging :as log]
   [zetawar.router.reagent]))

(defmethod ig/init-key :zetawar.system/reagent [_ opts]
  {:handler-wrapper-fn zetawar.router.reagent/handler-wrapper-fn})
