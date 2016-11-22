(ns zetawar.components.app
  (:require
   [com.stuartsierra.component :as component]
   [posh.core :as posh]
   [zetawar.router :as router]))

(defrecord App [datascript router timbre conn ev-chan notify-pub players locale]
  component/Lifecycle
  (start [component]
    (let [{:keys [conn]} datascript
          {:keys [ev-chan notify-pub players]} router
          dispatch #(router/dispatch ev-chan %)]
      (assoc component
             :conn conn
             :ev-chan ev-chan
             :notify-pub notify-pub
             :dispatch dispatch
             :players players
             :locale :en)))
  (stop [component]
    (assoc component
           :datascript nil
           :conn nil
           :ev-chan nil
           :dispatch nil
           :players nil
           :locale nil)))

(defn new-app
  []
  (component/using (map->App {})
                   [:datascript :router :timbre]))
