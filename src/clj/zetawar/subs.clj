(ns zetawar.subs)

;; TODO: check that tracks defined this way are only evaluated once per arg combination
;; TODO: add support for doc strings
(defmacro deftrack [name params* & body]
  `(def ~name
     (partial r/track (fn ~params* ~@body))))
