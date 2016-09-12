(ns zetawar.router
  (:require
   [cljs.core.async :refer [chan close! put!]]
   [datascript.core :as d]
   [taoensso.timbre :as log])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defmulti handle-event (fn [ev-ctx [ev-id & _]] ev-id))

;; TODO: add sync dispatch (?)

(defn dispatch! [ev-chan ev-msg]
  (log/debugf "Dispatching: %s" (pr-str ev-msg))
  (put! ev-chan ev-msg))

(defn handle-event* [{:as router-ctx :keys [ev-chan conn]} ev-msg]
  (let [ev-ctx (assoc router-ctx :db @conn)
        {:as ret :keys [tx dispatch]} (handle-event ev-ctx ev-msg)]
    (log/tracef "Handler returned: %s" (pr-str ret))
    (when tx
      (log/debugf "Transacting: %s" (pr-str tx))
      (d/transact! conn tx))
    (doseq [ret-ev-msg dispatch]
      (dispatch! ev-chan ret-ev-msg))))

(defn start [{:as router-ctx :keys [ev-chan conn]}]
  (go-loop [ev-msg (<! ev-chan)]
    (when ev-msg
      (log/debugf "Handling: %s" (pr-str ev-msg))
      ;; TODO: validate event
      ;; TODO: validate handler return value
      (handle-event* router-ctx ev-msg)
      (recur (<! ev-chan)))))
