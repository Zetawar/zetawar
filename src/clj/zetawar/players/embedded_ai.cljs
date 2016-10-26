(ns zetawar.players.embedded-ai
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :as db :refer [e qe qes qess]]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
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

(defrecord SimpleAIPlayer [faction-color ev-chan notify-pub player-chan conn]
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

(defmethod players/new-player ::players/embedded-ai
  [{:as app-ctx :keys [ev-chan notify-pub]} player-type faction-color]
  (let [player-chan (async/chan (async/dropping-buffer 10))
        conn (d/create-conn db/schema)]
    (SimpleAIPlayer. faction-color ev-chan notify-pub player-chan conn)))

(defmethod handle-event ::players/start-turn
  [{:as player :keys [faction-color]} _]
  {:dispatch [[:zetawar.events.player/send-game-state faction-color]]})

(defmethod handle-event ::players/apply-action
  [{:as player :keys [db faction-color]} [_ _ action]]
  (when (and (= faction-color (:action/faction-color action))
             (not= (:action/type action) :action.type/end-turn))
    {:dispatch [[:zetawar.events.player/send-game-state faction-color]]}))

(defn actor-score-fn [db game]
  (fn [actor]
    (cond
      (game/unit? actor) (rand-int 100)
      (game/base? actor) (+ (rand-int 100) 100))))

(defn choose-actor [db game]
  (let [actor-score (memoize (actor-score-fn db game))]
    (->> (game/actionable-actors db game)
         (apply max-key actor-score))))

(defn base-action-score-fn [db game base]
  (fn [action]
    (rand-int 200)))

(defn choose-base-action [db game base]
  (let [base-action-score (memoize (base-action-score-fn db game base))]
    (->> (game/base-actions db game base)
         (apply max-key base-action-score))))

(defn unit-action-score-fn [db game unit]
  (let [closest-base (game/closest-capturable-base db game unit)]
    (fn [action]
      (case (:action/type action)
        :action.type/capture-base
        200

        :action.type/attack-unit
        100

        :action.type/move-unit
        (let [[base-q base-r] (game/terrain-hex closest-base)
              {:keys [action/to-q action/to-r]} action
              base-distance (hex/distance base-q base-r to-q to-r)]
          (- 100 base-distance))

        0))))

(defn choose-unit-action [db game unit]
  (let [unit-action-score (memoize (unit-action-score-fn db game unit))]
    (->> (game/unit-actions db game unit)
         (apply max-key unit-action-score))))

(defn choose-action [player db game]
  (when-let [actor (choose-actor db game)]
    (cond
      (game/base? actor)
      (choose-base-action db game actor)

      (game/unit? actor)
      (choose-unit-action db game actor))))

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn faction-color]} [_ _ game-state]]
  (let [new-conn (d/create-conn db/schema)
        game-id (players/load-player-game-state! new-conn game-state)]
    (reset! conn @new-conn)
    (let [db @conn
          game (game/game-by-id db game-id)
          action (or (choose-action player db game)
                     {:action/type :action.type/end-turn
                      :action/faction-color faction-color})]
      {:dispatch [[:zetawar.events.player/execute-action action]]})))
