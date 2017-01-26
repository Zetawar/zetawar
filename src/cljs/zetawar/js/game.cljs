(ns zetawar.js.game
  (:require
   [zetawar.game :as game]))

(defn ^:export is_unit [x]
  (game/unit? x))

(defn ^:export is_base [x]
  (game/base? x))

(defn ^:export terrain_hex [terrain]
  (clj->js (game/terrain-hex terrain)))

(defn ^:export closest_capturable_base [db game unit]
  (game/closest-capturable-base db game unit))

