(ns zetawar.system.router
  (:require
   [cljs.core.async :as async]
   [integrant.core :as ig]
   [zetawar.router :as router]))

(defmethod ig/init-key :zetawar.system/router [_ opts]
  (let [{:keys [datascript players]} opts
        {:keys [conn]} datascript
        ;; TODO: figure out correct size for buffer
        ev-chan (async/chan (async/dropping-buffer 10))
        notify-chan (async/chan)
        notify-pub (async/pub notify-chan #(nth % 1))]
    (router/start {:ev-chan ev-chan
                   :notify-chan notify-chan
                   :notify-pub notify-pub
                   :conn conn
                   :players players})
    {:ev-chan ev-chan
     :notify-chan notify-chan
     :notify-pub notify-pub}))

(defmethod ig/halt-key! :zetawar.system/router [_ router]
  (let [{:keys [ev-chan notify-chan notify-pub]} router]
    (async/close! ev-chan)
    (async/close! notify-chan)))
