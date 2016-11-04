(ns zetawar.db
  (:require
   [datascript.core :as d]
   [zetawar.util :refer [breakpoint inspect solo ssolo]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Schema

(def schema
  {:db/ident                     {:db/unique      :db.unique/identity}

   ;; App
   :app/game                     {:db/valueType   :db.type/ref}

   ;; Game
   :game/id                      {:db/unique      :db.unique/identity}
   :game/map                     {:db/valueType   :db.type/ref}
   :game/factions                {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many
                                  :db/isComponent true}
   :game/starting-faction        {:db/valueType   :db.type/ref}
   :game/current-faction         {:db/valueType   :db.type/ref}

   ;; Faction
   :faction/next-faction         {:db/valueType   :db.type/ref}
   :faction/units                {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many
                                  :db/isComponent true
                                  :db/index       true}

   ;; Unit
   :unit/type                    {:db/valueType   :db.type/ref}
   :unit/game-pos-idx            {:db/unique      :db.unique/identity}
   :unit/attacked-units          {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many}
   ;; TODO: add unit/terrain
   ;; unit/state

   ;; Unit type
   :unit-type/id                 {:db/unique      :db.unique/identity}
   :unit-type/name               {:db/unique      :db.unique/identity}
   ;; unit-type/state-map

   ;; Unit State
   ;; - unit-state-map
   ;;   - unit-state-map/id
   ;;   - unit-state-map/states
   ;;   - unit-state-map/just-built-state
   ;;   - unit-state-map/start-turn-state
   ;; - unit-state
   ;;   - unit-state/name
   ;;   - unit-state/transititons
   ;; - unit-state-transition
   ;;   - unit-state-transition/action-type (-id?)
   ;;   - unit-state-transition/new-state

   ;; Map
   :map/terrains                 {:db/valueType   :db.type/ref
                                  :db/cardinality :db.cardinality/many
                                  :db/isComponent true}

   ;; Terrain
   :terrain/owner                {:db/valueType   :db.type/ref}
   :terrain/type                 {:db/valueType   :db.type/ref}
   :terrain/game-pos-idx         {:db/unique      :db.unique/identity}

   ;; Terrain type
   :terrain-type/id              {:db/unique      :db.unique/identity}
   :terrain-type/name            {:db/unique      :db.unique/identity}

   ;; Unit strength
   :unit-strength/unit-type      {:db/valueType   :db.type/ref}

   ;; Terrain effects
   :terrain-effect/unit-type     {:db/valueType   :db.type/ref
                                  :db/index       true}
   :terrain-effect/terrain-type  {:db/valueType   :db.type/ref
                                  :db/index       true}})

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Utils

(defn qe
  "Returns the single entity returned by a query."
  [query db & args]
  (when-let [result (-> (apply d/q query db args) ssolo)]
    (d/entity db result)))

;; TODO: add single arity version for singleton entities
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

(defn qess
  "Return a sequence of entities returned by a query, assuming
  that each :find result contains a single entity id."
  [query db & args]
  (->> (apply d/q query db args)
       (mapv #(d/entity db (solo %)))))

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
