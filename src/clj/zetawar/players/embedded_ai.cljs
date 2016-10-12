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

(defn get-game [db]
  (qe '[:find ?g
        :where
        [?g :game/id]]
      db))

;; TODO: need a can-act? predicate
(defn pick-unit [db game]
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
(defn unit-action [db game unit]
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

(defn build-action [db game]
  (let [current-faction (:game/current-faction game)
        owned-bases (->> (qes '[:find ?t
                                :in $ ?g
                                :where
                                [?g  :game/map ?m]
                                [?m  :map/terrains ?t]
                                [?t  :terrain/type ?tt]
                                [?tt :terrain-type/id :terrain-type.id/base]]
                              db (e game))
                         (apply concat)
                         (filter #(= current-faction (:terrain/owner %)))
                         (remove #(game/unit-at db game (:terrain/q %) (:terrain/r %)))
                         (into #{}))
        unit-types (game/buildable-unit-types db game)
        base (-> owned-bases shuffle first)
        unit-type (-> unit-types shuffle first)]
    (when (and unit-type base
               (not (game/unit-at db game (:terrain/q base) (:terrain/r base))))
      [:zetawar.events.game/build-unit (:terrain/q base) (:terrain/r base) (:unit-type/id unit-type)])))

(defmethod handle-event ::players/update-game-state
  [{:as player :keys [conn]} [_ faction-color game-state]]
  (when (= faction-color (:faction-color player))
    (let [new-conn (d/create-conn db/schema)]
      (game/load-specs! new-conn)
      (game/load-game-state! new-conn data/map-definitions data/scenario-definitions game-state)
      (reset! conn @new-conn)

      (let [db @conn
            game (get-game db)
            unit (pick-unit db game)]
        (if-let [b-action (build-action db game)]
          {:dispatch [b-action
                      [:zetawar.events.player/send-game-state (:faction-color player)]]}
          (if-let [u-action (when unit (unit-action db game unit))]
            {:dispatch [u-action
                        [:zetawar.events.player/send-game-state (:faction-color player)]]}
            {:dispatch [[:zetawar.events.ui/end-turn (:faction-color player)]]})))

      )))
