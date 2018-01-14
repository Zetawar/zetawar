(ns zetawar.hex
  (:require
   [zetawar.util :refer [abs]]))

(def min-q 0)
(def min-r 0)

(def max-q 100)
(def max-r 100)

(defn east [q r]
  [(inc q) r])

(defn east-of? [q1 r1 q2 r2]
  (= [q2 r2] (east q1 r1)))

(defn west [q r]
  [(dec q) r])

(defn west-of? [q1 r1 q2 r2]
  (= [q2 r2] (west q1 r1)))

(defn northeast [q r]
  (if (= (mod r 2) 0)
    [q (dec r)]
    [(inc q) (dec r)]))

(defn northeast-of? [q1 r1 q2 r2]
  (= [q2 r2] (northeast q1 r1)))

(defn northwest [q r]
  (if (= (mod r 2) 0)
    [(dec q) (dec r)]
    [q (dec r)]))

(defn northwest-of? [q1 r1 q2 r2]
  (= [q2 r2] (northwest q1 r1)))

(defn southeast [q r]
  (if (= (mod r 2) 0)
    [q (inc r)]
    [(inc q) (inc r)]))

(defn southeast-of? [q1 r1 q2 r2]
  (= [q2 r2] (southeast q1 r1)))

(defn southwest [q r]
  (if (= (mod r 2) 0)
    [(dec q) (inc r)]
    [q (inc r)]))

(defn southwest-of? [q1 r1 q2 r2]
  (= [q2 r2] (southwest q1 r1)))

;; TODO: make this more elegant
(defn opposite? [q1 r1 q2 r2 q3 r3]
  (or (and (east-of? q1 r1 q2 r2) (east-of? q2 r2 q3 r3))
      (and (west-of? q1 r1 q2 r2) (west-of? q2 r2 q3 r3))
      (and (northwest-of? q1 r1 q2 r2) (northwest-of? q2 r2 q3 r3))
      (and (southwest-of? q1 r1 q2 r2) (southwest-of? q2 r2 q3 r3))
      (and (northeast-of? q1 r1 q2 r2) (northeast-of? q2 r2 q3 r3))
      (and (southeast-of? q1 r1 q2 r2) (southeast-of? q2 r2 q3 r3))))

(def adjacents
  (memoize (fn adjacents [q r]
             #{(east q r) (west q r)
               (northeast q r) (northwest q r)
               (southeast q r) (southwest q r)})))

(defn adjacent? [q1 r1 q2 r2]
  ((adjacents q1 r1) [q2 r2]))

(defn offset->cube [q r]
  (let [x (- q (/ (- r (mod r 2)) 2))
        z r
        y (- 0 x z)]
    [x y z]))

(defn distance [q1 r1 q2 r2]
  (let [[x1 y1 z1] (offset->cube q1 r1)
        [x2 y2 z2] (offset->cube q2 r2)]
    (max (abs (- x1 x2))
         (abs (- y1 y2))
         (abs (- z1 z2)))))
