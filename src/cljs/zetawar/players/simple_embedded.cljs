(ns zetawar.players.simple-embedded
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [zetawar.logging :as log]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.game :as game]
   [zetawar.players :as players]
   [zetawar.router :as router])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

(defmulti handle-event (fn [player [ev-type & _]] ev-type))

(defmethod handle-event :default
  [_ msg]
  (log/debug "Unhandled player event:" (pr-str msg)))

(defn handle-event* [{:as player :keys [ev-chan]} msg]
  (let [{:as ret :keys [tx]} (handle-event player msg)]
    (log/trace "Player handler returned:" (pr-str ret))
    (doseq [new-msg (:dispatch ret)]
      (router/dispatch ev-chan new-msg))))

(defrecord SimpleEmbeddedPlayer [faction-color ev-chan notify-pub player-chan conn fns]
  players/Player
  (start [player]
    (let [{:keys [notify-pub]} player]
      (async/sub notify-pub :faction.color/all player-chan)
      (async/sub notify-pub faction-color player-chan)
      (go-loop [msg (<! player-chan)]
        (when msg
          (log/debug (str "[" faction-color "]") "Handling player event:"  (pr-str msg))
          ;; TODO: validate event
          ;; TODO: validate handler return value
          ;; TODO: catch exceptions
          (handle-event* player msg)
          (recur (<! player-chan))))))
  (stop [player]
    (async/close! player-chan)))

(defn simple-embedded-player [faction-color ev-chan notify-pub fns]
  (let [player-chan (async/chan (async/dropping-buffer 10))
        conn (d/create-conn db/schema)]
    (SimpleEmbeddedPlayer. faction-color ev-chan notify-pub player-chan conn fns)))

(defmethod handle-event ::players/start-turn
  [{:as player :keys [faction-color]} _]
  {:dispatch [[:zetawar.events.player/send-game-state faction-color]]})

;; TODO: apply actions incrementally instead of loading complete state every time
(defmethod handle-event ::players/apply-action
  [{:as player :keys [db faction-color]} [_ _ action]]
  (when (and (= faction-color (:action/faction-color action))
             (not= (:action/type action) :action.type/end-turn))
    {:dispatch [[:zetawar.events.player/send-game-state faction-color]]}))

(declare ^:dynamic *mk-actor-ctx*)

(declare ^:dynamic *score-actor*)

(defn choose-actor [db game ctx]
  (->> (game/actionable-actors db game)
       (keep (juxt identity #(*score-actor* db game ctx %)))
       (apply max-key second)
       first))

(declare ^:dynamic *mk-base-action-ctx*)

(declare ^:dynamic *score-base-action*)

(defn choose-base-action [db game base action-ctx]
  (->> (game/base-actions db game base)
       (keep (juxt identity #(*score-base-action* db game base action-ctx %)))
       (apply max-key second)
       first))

(declare ^:dynamic *mk-unit-action-ctx*)

(declare ^:dynamic *score-unit-action*)

(defn choose-unit-action [db game unit action-ctx]
  (->> (game/unit-actions db game unit)
       (keep (juxt identity #(*score-unit-action* db game unit action-ctx %)))
       (apply max-key second)
       first))

(defn choose-action [player db game]
  (let [actor-ctx (*mk-actor-ctx* db game)
        actor (choose-actor db game actor-ctx)]
    (when actor
      (cond
        (game/base? actor)
        (let [action-ctx (*mk-base-action-ctx* db game actor-ctx actor)]
          (choose-base-action db game actor action-ctx))

        (game/unit? actor)
        (let [action-ctx (*mk-unit-action-ctx* db game actor-ctx actor)]
          (choose-unit-action db game actor action-ctx))))))

(defn wrap-exception-handler [f]
  (fn [& args]
    (try
      (apply f args)
      (catch :default ex
        (log/error "Error running player function:" ex)
        nil))))

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn faction-color fns]} [_ _ game-state]]
  (let [new-conn (d/create-conn db/schema)
        game-id (players/load-player-game-state! new-conn game-state)]
    (reset! conn @new-conn)
    (let [db @conn
          game (game/game-by-id db game-id)
          {:keys [mk-actor-ctx score-actor
                  mk-base-action-ctx score-base-action
                  mk-unit-action-ctx score-unit-action]} fns
          action (or (binding [*mk-actor-ctx* (wrap-exception-handler (or mk-actor-ctx (constantly nil)))
                               *score-actor* (wrap-exception-handler score-actor)
                               *mk-base-action-ctx* (wrap-exception-handler (or mk-base-action-ctx (fn [db game actor-ctx base] actor-ctx)))
                               *score-base-action* (wrap-exception-handler score-base-action)
                               *mk-unit-action-ctx* (wrap-exception-handler (or mk-unit-action-ctx (fn [db game actor-ctx unit] actor-ctx)))
                               *score-unit-action* (wrap-exception-handler score-unit-action)]
                       (choose-action player db game))
                     {:action/type :action.type/end-turn
                      :action/faction-color faction-color})]
      {:dispatch [[:zetawar.events.player/execute-action action]]})))
