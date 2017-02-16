(ns zetawar.js.game
  (:require
   [zetawar.game :as game]))

(defn ^:export is_unit [x]
  (game/unit? x))

(defn ^:export is_base [x]
  (game/base? x))

(defn ^:export terrain_hex [terrain]
  (clj->js (game/terrain-hex terrain)))

(defn ^:export unit_hex [terrain]
  (clj->js (game/unit-hex terrain)))

(defn ^:export closest_capturable_base [db game unit]
  (game/closest-capturable-base db game unit))

(defn ^:export closest_enemy [db game unit]
  (game/closest-enemy db game unit))
