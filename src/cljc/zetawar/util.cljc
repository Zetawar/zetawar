(ns zetawar.util
  #?(:cljs
     (:require-macros [zetawar.util :refer [inspect]])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Accessors

(defn solo
  "Like first, but throws if more than one item."
  [coll]
  (assert (not (next coll)))
  (first coll))

(defn only
  "Like first, but throws unless exactly one item."
  [coll]
  (assert (not (next coll)))
  (if-let [result (first coll)]
    result
    (assert false)))

(defn ssolo
  "Same as (solo (solo coll))."
  [coll]
  (solo (solo coll)))

(defn oonly
  "Same as (only (only coll))."
  [coll]
  (only (only coll)))

(defn select-values
  "Returns a vector containing only those values who's key is in ks."
  [m ks]
  (reduce #(if-let [v (m %2)] (conj %1 v) %1) [] ks))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Math

(defn abs [x]
  (#?(:clj Math/abs :cljs js/Math.abs) x))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Debugging

(defn log-inspect [expr result]
  #?(:cljs (js/console.debug expr result)))


(defn- inspect-1 [expr]
  `(let [result# ~expr]
     (zetawar.util/log-inspect '~expr result#)
     result#))

#?(:clj
   (do

     (defmacro inspect [& exprs]
       `(do ~@(map inspect-1 exprs)))

     (defmacro breakpoint []
       '(do (js* "debugger;")
            nil)) ; (prevent "return debugger;" in compiled javascript)

     )
   )
