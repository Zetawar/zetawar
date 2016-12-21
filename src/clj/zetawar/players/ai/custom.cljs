(ns zetawar.players.ai.custom
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.players :as players]
   [zetawar.players.simple-embedded :refer [simple-embedded-player]]))

(defn action-ctx [db game]
  {})

(defn actor-score-fn [db game ctx]
  (fn [actor]
    (cond
      (game/unit? actor) (rand-int 100)
      (game/base? actor) (+ (rand-int 100) 100))))

(defn base-action-score-fn [db game ctx base]
  (fn [action]
    (rand-int 200)))

(defn unit-action-score-fn [db game ctx unit]
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

(defmethod players/new-player ::players/custom-ai
  [{:as app-ctx :keys [ev-chan notify-pub]} player-type faction-color]
  (let [fns {:action-ctx #'action-ctx
             :actor-score-fn #'actor-score-fn
             :base-action-score-fn #'base-action-score-fn
             :unit-action-score-fn #'unit-action-score-fn}]
    (simple-embedded-player faction-color
                            ev-chan
                            notify-pub
                            fns)))
