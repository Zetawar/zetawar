(ns zetawar.util
  (:require
    [fipp.edn]))

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
  "Same as (solo (solo coll))"
  [coll]
  (solo (solo coll)))

(defn oonly
  "Same as (only (only coll))"
  [coll]
  (only (only coll)))

;; TODO: check performance
(defn select-values [m ks]
  (reduce #(if-let [v (m %2)] (conj %1 v) %1) [] ks))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Debugging

;; TODO: refer to https://github.com/shaunlebron/How-To-Debug-CLJS/blob/master/src/example/macros.clj

(defn spy [x]
  (js/console.debug
    (with-out-str
      (fipp.edn/pprint x)))
  x)
