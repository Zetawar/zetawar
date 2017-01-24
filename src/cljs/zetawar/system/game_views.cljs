(ns zetawar.system.game-views
  (:require
   [integrant.core :as ig]
   [posh.reagent :as posh]
   [taoensso.timbre :as log]
   [tongue.core :as tongue]
   [zetawar.data :as data]
   [zetawar.router :as router]))

(defmethod ig/init-key :zetawar.system/game-views [_ opts]
  (let [{:keys [datascript players router locale]} opts
        {:keys [conn]} datascript
        {:keys [ev-chan notify-pub]} router
        dispatch #(router/dispatch ev-chan %)
        translate (-> data/dicts
                      tongue/build-translate
                      (partial locale))]
    (posh/posh! conn)
    {:conn conn
     :dispatch dispatch
     :translate translate}))

;; TODO: implement resume-key
