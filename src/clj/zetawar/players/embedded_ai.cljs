(ns zetawar.players.embedded-ai
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [taoensso.timbre :as log]
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
  (log/debugf "Unhandled player event: %s" (pr-str msg)))

(defn handle-event* [{:as player :keys [ev-chan]} msg]
  (let [{:as ret :keys [tx]} (handle-event player msg)]
    (log/tracef "Player handler returned: %s" (pr-str ret))
    (doseq [new-msg (:dispatch ret)]
      (router/dispatch ev-chan new-msg))))

(defrecord SimpleEmbeddedAIPlayer [faction-color ev-chan notify-pub player-chan conn fns]
  players/Player
  (start [player]
    (let [{:keys [notify-pub]} player]
      (async/sub notify-pub :faction.color/all player-chan)
      (async/sub notify-pub faction-color player-chan)
      (go-loop [msg (<! player-chan)]
        (when msg
          (log/debugf "Handling player event: %s" (pr-str msg))
          ;; TODO: validate event
          ;; TODO: validate handler return value
          ;; TODO: catch exceptions
          (handle-event* player msg)
          (recur (<! player-chan))))))
  (stop [player]
    (async/close! player-chan)))

(defn new-simple-embedded-ai-player [faction-color ev-chan notify-pub fns]
  (let [player-chan (async/chan (async/dropping-buffer 10))
        conn (d/create-conn db/schema)]
    (SimpleEmbeddedAIPlayer. faction-color ev-chan notify-pub player-chan conn fns)))

(defmethod handle-event ::players/start-turn
  [{:as player :keys [faction-color]} _]
  {:dispatch [[:zetawar.events.player/send-game-state faction-color]]})

(defmethod handle-event ::players/apply-action
  [{:as player :keys [db faction-color]} [_ _ action]]
  (when (and (= faction-color (:action/faction-color action))
             (not= (:action/type action) :action.type/end-turn))
    {:dispatch [[:zetawar.events.player/send-game-state faction-color]]}))

(declare ^:dynamic *action-ctx*)

(declare ^:dynamic *actor-score-fn*)

;; TODO: cleanup choose-* functions
(defn choose-actor [db game ctx]
  (let [actor-score (memoize (*actor-score-fn* db game ctx))]
    (->> (game/actionable-actors db game)
         (keep (juxt identity actor-score))
         (apply max-key second)
         first)))

(declare ^:dynamic *base-action-score-fn*)

(defn choose-base-action [db game ctx base]
  (let [base-action-score (memoize (*base-action-score-fn* db game ctx base))]
    (->> (game/base-actions db game base)
         (keep (juxt identity base-action-score))
         (apply max-key second)
         first)))

(declare ^:dynamic *unit-action-score-fn*)

(defn choose-unit-action [db game ctx unit]
  (let [unit-action-score (memoize (*unit-action-score-fn* db game ctx unit))]
    (->> (game/unit-actions db game unit)
         (keep (juxt identity unit-action-score))
         (apply max-key second)
         first)))

(defn choose-action [player db game]
  (let [ctx (*action-ctx* db game)
        actor (choose-actor db game ctx)]
    (when actor
      (cond
        (game/base? actor)
        (choose-base-action db game ctx actor)

        (game/unit? actor)
        (choose-unit-action db game ctx actor)))))

(defn wrap-exception-handler [f]
  (fn [& args]
    (let [f' (apply f args)]
      (fn [& args']
        (try
          (apply f' args')
          (catch :default ex
            (log/error "Error running AI function:" ex)
            nil))))))

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn faction-color fns]} [_ _ game-state]]
  (let [new-conn (d/create-conn db/schema)
        game-id (players/load-player-game-state! new-conn game-state)]
    (reset! conn @new-conn)
    (let [db @conn
          game (game/game-by-id db game-id)
          {:keys [action-ctx actor-score-fn base-action-score-fn unit-action-score-fn]} fns
          action (or (binding [*action-ctx* (wrap-exception-handler (or action-ctx (constantly nil)))
                               *actor-score-fn* (wrap-exception-handler actor-score-fn)
                               *base-action-score-fn* (wrap-exception-handler base-action-score-fn)
                               *unit-action-score-fn* (wrap-exception-handler unit-action-score-fn)]
                       (choose-action player db game))
                     {:action/type :action.type/end-turn
                      :action/faction-color faction-color})]
      {:dispatch [[:zetawar.events.player/execute-action action]]})))
