(ns zetawar.util
  (:require
    [datascript.core :as d]
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
;;; DB

(defn qe
  "Returns the single entity returned by a query."
  [query db & args]
  (when-let [result (-> (apply d/q query db args) ssolo)]
    (d/entity db result)))

(defn find-by
  "Returns the unique entity identified by attr and val."
  [db attr val]
  (qe '[:find ?e
        :in $ ?attr ?val
        :where  [?e ?attr ?val]]
      db attr val))

(defn qes
  "Returns the entities returned by a query, assuming that
   all :find results are entity ids."
  [query db & args]
  (->> (apply d/q query db args)
       (mapv (fn [items]
               (mapv (partial d/entity db) items)))))

(defprotocol Eid
  "A simple protocol for retrieving an object's id."
  (e [_] "identifying id for a value"))

(extend-protocol Eid
  number
  (e [n] n)

  cljs.core.PersistentHashMap
  (e [ent] (:db/id ent))

  cljs.core.PersistentArrayMap
  (e [ent] (:db/id ent))

  datascript.impl.entity.Entity
  (e [ent] (:db/id ent)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Debugging

;; TODO: refer to https://github.com/shaunlebron/How-To-Debug-CLJS/blob/master/src/example/macros.clj

(defn spy [x]
  (js/console.debug
    (with-out-str
      (fipp.edn/pprint x)))
  x)
