(ns zetawar.players.embedded-ai
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :as db :refer [e qe qes qess]]
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

(defn build-at-score-fn [db game]
  (fn [base]
    (rand)))

(defn to-build-score-fn [db game]
  (fn [unit-type]
    (rand)))

(defn choose-build-action [player db game]
  (let [build-at-score (build-at-score-fn db game)
        to-build-score (to-build-score-fn db game)
        cur-faction (:game/current-faction game)
        base (->> (game/faction-bases db cur-faction)
                  (remove #(game/unit-at db game (:terrain/q %) (:terrain/r %)))
                  (sort-by (memoize #(build-at-score %)))
                  first)
        [base-q base-r] (game/terrain-hex base)
        unit-type (->> (game/buildable-unit-types db game)
                       (sort-by (memoize #(to-build-score %)))
                       first)]
    (when (and base unit-type (not (game/unit-at db game base-q base-r)))
      [:zetawar.events.player/build-unit
       (:faction-color player) base-q base-r (:unit-type/id unit-type)])))

(defn act-score-fn [db game]
  (fn [unit]
    (rand)))

(defn choose-unit [db game]
  (let [act-score (act-score-fn db game)]
    (->> game
         :game/current-faction
         :faction/units
         (filter #(game/unit-can-act? db game %))
         (sort-by (memoize #(act-score %)))
         first)))

;; TODO: generalize action code; return list of all actions and compute score for each

(defn chose-move [db game unit]
  (let [closest-base (game/closest-capturable-base db game unit)]
    (game/closest-move-to-hex db game unit (:terrain/q closest-base) (:terrain/r closest-base))))

(defn target-score-fn [db game unit]
  (fn [target]
    (rand)))

(defn choose-target [db game unit]
  (let [target-score (target-score-fn db game unit)]
    (->> (game/enemies-in-range db game unit)
         (sort-by (memoize #(target-score %)))
         first)))

(defn choose-unit-action [player db game unit]
  (let [terrain (game/unit-terrain db game unit)]
    (if (and (game/on-capturable-base? db game unit)
             (game/can-capture? db game unit terrain))
      [:zetawar.events.player/capture-base (:faction-color player) (:unit/q unit) (:unit/r unit)]
      (if-let [move (when (game/can-move? db game unit)
                      (not-empty (chose-move db game unit)))]
        (into [:zetawar.events.player/move-unit (:faction-color player)] (concat (:from move) (:to move)))
        (when-let [target (when (game/can-attack? db game unit)
                            (choose-target db game unit))]
          [:zetawar.events.player/attack-unit
           (:faction-color player) (:unit/q unit) (:unit/r unit) (:unit/q target) (:unit/r target)])))))

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn faction-color]} [_ _ game-state]]
  (let [new-conn (d/create-conn db/schema)
        game-id (players/load-player-game-state! new-conn game-state)]
    (reset! conn @new-conn)
    (let [db @conn
          game (game/game-by-id db game-id)]
      (if-let [build-action (choose-build-action player db game)]
        {:dispatch [build-action]}
        (let [unit (choose-unit db game)]
          (if-let [unit-action (and unit (choose-unit-action player db game unit))]
            {:dispatch [unit-action]}
            {:dispatch [[:zetawar.events.player/end-turn faction-color]]}))))))
