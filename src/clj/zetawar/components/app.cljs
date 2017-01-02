(ns zetawar.components.app
  (:require
   [com.stuartsierra.component :as component]
   [posh.core :as posh]
   [tongue.core :as tongue]
   [zetawar.data :as data]
   [zetawar.router :as router]))

(defrecord App [datascript router timbre conn ev-chan notify-pub players locale translate]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript
          {:keys [ev-chan notify-pub players]} router
          dispatch #(router/dispatch ev-chan %)
          translate (-> data/dicts
                        tongue/build-translate
                        (partial locale))]
      (assoc component
             :conn conn
             :ev-chan ev-chan
             :notify-pub notify-pub
             :dispatch dispatch
             :players players
             :locale locale
             :translate translate)))
  (stop [component]
    (assoc component
           :datascript nil
           :conn nil
           :ev-chan nil
           :dispatch nil
           :players nil
           :locale nil
           :translate nil)))

(defn new-app
  []
  (component/using (map->App {:locale :en})
                   [:datascript :router :timbre]))
