(ns zetawar.db
  (:require
   [zetawar.util :refer [ssolo]]
   [datascript.core :as d]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; ## Schema

;; TODO: add unit/terrain
(def schema
  {:db/ident                     {:db/unique      :db.unique/identity}
   :app/game                     {:db/valueType   :db.type/ref}
   :game/id                      {:db/unique      :db.unique/identity}
   :game/map                     {:db/valueType   :db.type/ref}
   :game/factions                {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many
                                  :db/isComponent true}
   :game/current-faction         {:db/valueType   :db.type/ref}
   :faction/next-faction         {:db/valueType   :db.type/ref}
   :faction/units                {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many
                                  :db/isComponent true
                                  :db/index       true}
   :unit/type                    {:db/valueType   :db.type/ref}
   :unit/game-pos-idx            {:db/unique      :db.unique/identity}
   :unit/attacked-units          {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many}
   :unit-type/id                 {:db/unique      :db.unique/identity}
   :unit-type/name               {:db/unique      :db.unique/identity}
   :map/starting-faction         {:db/valueType   :db.type/ref}
   :map/terrains                 {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many
                                  :db/isComponent true}
   :terrain/owner                {:db/valueType   :db.type/ref}
   :terrain/type                 {:db/valueType   :db.type/ref}
   :terrain/game-pos-idx         {:db/unique      :db.unique/identity}
   :terrain-type/id              {:db/unique      :db.unique/identity}
   :terrain-type/name            {:db/unique      :db.unique/identity}
   :unit-strength/unit-type      {:db/valueType   :db.type/ref}
   :terrain-effect/unit-type     {:db/valueType   :db.type/ref
                                  :db/index       true}
   :terrain-effect/terrain-type  {:db/valueType   :db.type/ref
                                  :db/index       true}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; ## Utility Functions

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

(defn find-all-by
  "Returns all entities possessing attr."
  [db attr]
  (qes '[:find ?e
         :in $ ?attr
         :where [?e ?attr]]
       db attr))

(defprotocol Eid
  "A protocol for retrieving an object's entity id."
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
