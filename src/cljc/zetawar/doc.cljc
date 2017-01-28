(ns zetawar.doc
  (:refer-clojure :exclude [def]))

(defonce ^:private registry-ref (atom {}))

(defn def
  "Given a namespace-qualified keyword k and a docstring, makes an entry in the
  registry mapping k to the docstring."
  [k docstring]
  (c/assert (c/and (keyword? k) (namespace k)) "k must be namespaced keyword")
  (swap! registry-ref assoc k docstring)
  k)

(defn registry
  "Returns the registry map, prefer 'get-docstring' to lookup a docstring by
  name."
  []
  @registry-ref)

(defn get-doc
  "Returns docstring registered for keyword or nil."
  [k]
  (get (registry) k))
