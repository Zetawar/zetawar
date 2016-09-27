(ns zetawar.hex)

(def min-q 0)
(def min-r 0)

(def max-q 100)
(def max-r 100)

(defn east [q r]
  [(inc q) r])

(defn west [q r]
  [(dec q) r])

(defn northeast [q r]
  (if (= (mod r 2) 0)
    [q (dec r)]
    [(inc q) (dec r)]))

(defn northwest [q r]
  (if (= (mod r 2) 0)
    [(dec q) (dec r)]
    [q (dec r)]))

(defn southeast [q r]
  (if (= (mod r 2) 0)
    [q (inc r)]
    [(inc q) (inc r)]))

(defn southwest [q r]
  (if (= (mod q 2) 0)
    [(dec q) (inc r)]
    [q (inc r)]))

(def adjacents
  (memoize (fn adjacents [q r]
             [(east q r) (west q r)
              (northeast q r) (northwest q r)
              (southeast q r) (southwest q r)])))

(defn offset->cube [q r]
  (let [x (- q (/ (- r (mod r 2)) 2))
        z r
        y (- 0 x z)]
    [x y z]))

(defn distance [q1 r1 q2 r2]
  (let [[x1 y1 z1] (offset->cube q1 r1)
        [x2 y2 z2] (offset->cube q2 r2)]
    (max (js/Math.abs (- x1 x2))
         (js/Math.abs (- y1 y2))
         (js/Math.abs (- z1 z2)))))

(def offset->pixel
  (memoize
    (fn [q r]
      (if (= 0 (mod r 2))
        [(* q 32) (* r 26)]
        [(+ 16 (* q 32)) (+ 26 (* (- r 1) 26))]))))
