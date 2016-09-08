(ns zetawar.router
  (:require
    [cljs.core.async :refer [chan close! put!]]
    [datascript.core :as d]
    [taoensso.timbre :as log])
  (:require-macros
    [cljs.core.async.macros :refer [go go-loop]]))

(defmulti handle-event (fn [ev-ctx [ev-id & _]] ev-id))

(defn dispatch! [ev-chan ev-msg]
  (log/debugf "Dispatching: %s" (pr-str ev-msg))
  (put! ev-chan ev-msg))

(defn start [{:as handler-ctx :keys [conn ev-chan]}]
  (go-loop [ev-msg (<! ev-chan)]
    (when ev-msg
      (log/debugf "Handling: %s" (pr-str ev-msg))
      ; TODO: validate event
      ; TODO: validate handler return value
      (let [{:as ret :keys [tx dispatch]} (handle-event {:db @conn} ev-msg)]
        (log/tracef "Handler returned: %s" (pr-str ret))
        (when tx
          (log/debugf "Transacting: %s" (pr-str tx))
          (d/transact! conn tx))
        (doseq [event dispatch]
          (dispatch! ev-chan event)))
      (recur (<! ev-chan)))))
