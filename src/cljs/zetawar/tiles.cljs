(ns zetawar.tiles)

(def width 32)
(def height 34)
(def row-offset 26)
(def odd-row-column-offset (/ width 2))

(def offset->pixel
  (memoize
   (fn [q r]
     [(cond-> (* q width)
        (odd? r) (+ odd-row-column-offset))
      (* r row-offset)])))
