(ns zetawar.components.app
  (:require
   [com.stuartsierra.component :as component]
   [posh.core :as posh]))

(defrecord App [datascript router timbre conn ev-chan notify-pub players locale]
  component/Lifecycle
  (start [component]
    (assoc component
           :conn (:conn datascript)
           :ev-chan (:ev-chan router)
           :notify-pub (:notify-pub router)
           :players (:players router)
           :locale :en))
  (stop [component]
    (assoc component
           :datascript nil
           :conn nil
           :ev-chan nil
           :players nil
           :locale nil)))

(defn new-app
  []
  (component/using (map->App {})
                   [:datascript :router :timbre]))
