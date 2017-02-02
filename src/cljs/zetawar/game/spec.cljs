(ns zetawar.game.spec
  (:require
   [clojure.spec :as s]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Maps

(s/def :game.map/description string?)
(s/def :game.map.terrain/q (s/and int? #(>= % 0)))
(s/def :game.map.terrain/r (s/and int? #(>= % 0)))
(s/def :game.map.terrain/terrain-type keyword?)

(s/def :game.map/terrain
  (s/keys :req-un [:game.map.terrain/q
                   :game.map.terrain/r
                   :game.map.terrain/terrain-type]))

(s/def :game.map/terrains
  (s/coll-of :game.map/terrain))

(s/def :game/map
  (s/keys :req-un [:game.map/description
                   :game.map/terrains]))
