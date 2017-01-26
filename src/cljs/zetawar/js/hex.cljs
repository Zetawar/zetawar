(ns zetawar.js.hex
  (:require
   [zetawar.hex :as hex]))

(defn ^:export distance [q1 r1 q2 r2]
  (hex/distance q1 r1 q2 r2))
