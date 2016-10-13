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

(defrecord EmbeddedAIPlayer [faction-color ev-chan notify-pub player-chan conn]
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

(defmethod players/new-player ::players/embeded-ai
  [{:as app-ctx :keys [ev-chan notify-pub]} player-type faction-color]
  (let [player-chan (async/chan (async/dropping-buffer 10))
        conn (d/create-conn db/schema)]
    (EmbeddedAIPlayer. faction-color ev-chan notify-pub player-chan conn)))

(defmethod handle-event ::players/start-turn
  [{:as player :keys [faction-color conn]} _]
  {:dispatch [[:zetawar.events.player/send-game-state faction-color]]})

;; TODO: add base-score-fn
;; TODO: add unit-type-score-fn

(defn choose-build-action [db game]
  (let [cur-faction (:game/current-faction game)
        possible-bases (into []
                             (remove #(game/unit-at db game (:terrain/q %) (:terrain/r %)))
                             (game/faction-bases db cur-faction))
        unit-types (game/buildable-unit-types db game)
        chosen-base (-> possible-bases shuffle first)
        chosen-unit-type (-> unit-types shuffle first)]
    (when (and chosen-base chosen-unit-type)
      [:zetawar.events.game/build-unit
       (:terrain/q chosen-base) (:terrain/r chosen-base) (:unit-type/id chosen-unit-type)])))

;; TODO: add unit-score-fn

;; TODO: need a can-act? predicate
(defn choose-unit [db game]
  (->> game
       :game/current-faction
       :faction/units
       (remove #(= (:game/round game) (:unit/round-built %)))
       (remove :unit/repaired)
       (remove :unit/capturing)
       (remove #(let [terrain (game/terrain-at db game (:unit/q %) (:unit/r %))]
                  (and (not (game/can-capture? db game % terrain))
                       (> (:unit/move-count %) 0))))
       shuffle
       first))

;; TODO: this is terrible, clean it up
(defn choose-unit-action [db game unit]
  (let [base (game/closest-capturable-base db game unit)
        move (game/closest-move-to-hex db game unit (:terrain/q base) (:terrain/r base))]
    (if (and (game/on-capturable-base? db game unit)
             (game/can-capture? db game unit base))
      [:zetawar.events.game/capture-base (:unit/q unit) (:unit/r unit)]
      (if (first move)
        (into [:zetawar.events.game/move-unit] (concat (:from move) (:to move)))
        (when-let [enemy (-> (game/enemies-in-range db game unit) shuffle first)]
          (when (game/can-attack? db game unit)
            [:zetawar.events.game/attack-unit (:unit/q unit) (:unit/r unit) (:unit/q enemy) (:unit/r enemy)]))))))

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn]} [_ faction-color game-state]]
  (when (= faction-color (:faction-color player))
    (let [new-conn (d/create-conn db/schema)
          game-id (players/load-player-game-state! new-conn game-state)]
      (reset! conn @new-conn)
      (let [db @conn
            game (game/game-by-id db game-id)]
        (if-let [build-action (choose-build-action db game)]
          {:dispatch [build-action
                      [:zetawar.events.player/send-game-state (:faction-color player)]]}
          (let [unit (choose-unit db game)]
            (if-let [unit-action (and unit (choose-unit-action db game unit))]
              {:dispatch [unit-action
                          [:zetawar.events.player/send-game-state (:faction-color player)]]}
              {:dispatch [[:zetawar.events.ui/end-turn (:faction-color player)]]})))))))
