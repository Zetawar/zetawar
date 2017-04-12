(ns zetawar.system.game-views
  (:require
   [integrant.core :as ig]
   [posh.reagent :as posh]
   [taoensso.timbre :as log]
   [tongue.core :as tongue]
   [zetawar.data :as data]
   [zetawar.router :as router]))

;; TODO: start Reagent components when running in the browser
(defmethod ig/init-key :zetawar.system/game-views [_ opts]
  (let [{:keys [datascript router locale]} opts
        {:keys [conn]} datascript
        {:keys [ev-chan]} router
        dispatch #(router/dispatch ev-chan %)
        translate (-> data/dicts
                      tongue/build-translate
                      (partial locale))]
    (posh/posh! conn)
    {:conn conn
     :dispatch dispatch
     :translate translate}))

(defmethod ig/resume-key :zetawar.system/game-views [_ opts old-opts old-impl]
  (let [{:keys [datascript router locale]} opts
        {:keys [conn]} datascript
        {:keys [ev-chan]} router
        old-conn (get-in old-opts [:datascript :conn])
        dispatch #(router/dispatch ev-chan %)
        translate (-> data/dicts
                      tongue/build-translate
                      (partial locale))]
    (when-not (= conn old-conn)
      (posh/posh! conn))
    {:conn conn
     :dispatch dispatch
     :translate translate}))
