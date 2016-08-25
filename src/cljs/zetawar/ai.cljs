(ns zetawar.ai
  (:require
    [cljs.pprint :refer [pprint]]
    [datascript.core :as d]
    [zetawar.db :refer [e qe qes]]
    [zetawar.game :as game]))

;; for each unit:
;; - repair if < 50% health (optional)
;; - get moves
;; - pick move that gets closest to a capturable base
;;   - only move units that can capture onto bases
;; - capture if on base
;; - get attacks
;; - attack a random enemy unit if possible

;; for each owned base:
;; - build a random unit

(defn move [conn game-id unit]
  (when-not (:unit/capturing unit)
    (let [db @conn
          game (qe '[:find ?g
                     :in $ ?game-id
                     :where
                     [?g :game/id ?game-id]]
                   db game-id)
          base (game/closest-capturable-base db game unit)
          move (game/closest-move-to-qr db game unit (:terrain/q base) (:terrain/r base))]
      (if (game/on-capturable-base? db game unit)
        (game/capture! conn game-id (:unit/q unit) (:unit/r unit))
        (when (first move) ; hack to deal with [nil nil]
          (apply game/move! conn game-id (concat (:from move) (:to move))))))))

(defn attack [conn game-id unit]
  (when-not (:unit/capturing unit)
    (let [db @conn
          game (qe '[:find ?g
                     :in $ ?game-id
                     :where
                     [?g :game/id ?game-id]]
                   db game-id)
          enemy (-> (game/enemies-in-range db game unit) shuffle first)]
      (when (and enemy (game/can-attack? db game unit))
        (game/attack! conn game-id
                      (:unit/q unit) (:unit/r unit)
                      (:unit/q enemy) (:unit/r enemy))))))

(defn build [conn game-id]
  (let [db @conn
        game (qe '[:find ?g
                   :in $ ?game-id
                   :where
                   [?g :game/id ?game-id]]
                 db game-id)
        current-faction (:game/current-faction game)
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
                         (into #{}))
        unit-types (game/buildable-unit-types db game)
        base (-> owned-bases shuffle first)
        unit-type (-> unit-types shuffle first)]
    (when (and unit-type base
               (not (game/unit-at db game (:terrain/q base) (:terrain/r base))))
      (game/build! conn game-id
                   (:terrain/q base) (:terrain/r base)
                   (:unit-type/id unit-type)))))

(defn execute-turn [conn game-id]
  (let [units (flatten (qes '[:find ?u
                              :where
                              [_  :game/current-faction ?f]
                              [?f :faction/units ?u]]
                            @conn))]
    (build conn game-id)
    (loop [[unit & units] units]
      (when unit
        (move conn game-id unit)
        (recur units))))
  (let [units (flatten (qes '[:find ?u
                              :where
                              [_  :game/current-faction ?f]
                              [?f :faction/units ?u]]
                            @conn))]
    (loop [[unit & units] units]
      (when unit
        (attack conn game-id unit)
        (recur units)))))
