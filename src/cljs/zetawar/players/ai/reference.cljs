(ns zetawar.players.ai.reference
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.logging :as log]
   [zetawar.players :as players]
   [zetawar.players.simple-embedded :refer [simple-embedded-player]]))

(defn score-actor [db game actor actor-ctx]
  (cond
    (game/unit? actor) (rand-int 100)
    (game/base? actor) (+ (rand-int 100) 100)))

(defn score-base-action [db game base action-ctx action]
  (rand-int 200))

(defn mk-unit-action-ctx [db game actor-ctx unit]
  (assoc actor-ctx
         :closest-base (game/closest-capturable-base db game unit)
         :closest-enemy (game/closest-enemy db game unit)))

(defn score-unit-action [db game unit action-ctx action]
  (let [{:keys [closest-base closest-enemy]} action-ctx]
    (case (:action/type action)
      :action.type/capture-base
      200

      :action.type/attack-unit
      100

      :action.type/move-unit
      (if closest-base
        (let [[base-q base-r] (game/terrain-hex closest-base)
              {:keys [action/to-q action/to-r]} action
              base-distance (hex/distance base-q base-r to-q to-r)]
          (- 100 base-distance))
        (let [[enemy-q enemy-r] (game/unit-hex closest-enemy)
              {:keys [action/to-q action/to-r]} action
              enemy-distance (hex/distance enemy-q enemy-r to-q to-r)]
          (- 100 enemy-distance)))

      0)))

(defmethod players/new-player ::players/reference-ai
  [{:as app-ctx :keys [ev-chan notify-pub]} player-type faction-color]
  (let [fns {:score-actor #'score-actor
             :score-base-action #'score-base-action
             :mk-unit-action-ctx #'mk-unit-action-ctx
             :score-unit-action #'score-unit-action}]
    (simple-embedded-player faction-color
                            ev-chan
                            notify-pub
                            fns)))
