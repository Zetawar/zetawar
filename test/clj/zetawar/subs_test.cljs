(ns zetawar.subs-test
  (:require
   [cljs.test :refer-macros [testing is async use-fixtures]]
   [datascript.core :as d]
   [devcards.core :as dc :refer-macros [deftest]]
   [zetawar.app :as app]
   [zetawar.db :refer [e qe]]
   [zetawar.game :as game]
   [zetawar.subs :as subs]
   [zetawar.test-helper :as helper]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; App

(deftest test-app-eid
  (let [conn (helper/create-aruba-conn)
        app-eid (ffirst (d/q '[:find ?a
                               :where
                               [?a :app/game]]
                             @conn))]
    (testing "returned app-eid matches queried app-eid"
      (is (= app-eid @(subs/app-eid conn))))))

(deftest test-app
  (let [conn (helper/create-aruba-conn)]
    (testing "returned app has expected attributes"
      (is (:app/game @(subs/app conn))))))

;; TODO: add show-win-message? test

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game

(deftest test-game-eid
  (let [conn (helper/create-aruba-conn)
        game-eid (ffirst (d/q '[:find ?g
                                :where
                                [_ :app/game ?g]]
                              @conn))]
    (testing "game-eid returned by subscription is the same as queried game-eid"
      (is (= game-eid @(subs/game-eid conn))))))

(deftest test-game
  (let [conn (helper/create-aruba-conn)]
    (testing "game returned by subscription has expected attributes"
      (is  (= #{:db/id
                :game/id
                :game/scenario-id
                :game/map
                :game/credits-per-base
                :game/max-count-per-unit
                :game/starting-faction
                :game/current-faction
                :game/factions
                :game/round}
              (into #{} (keys @(subs/game conn))))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Map

(deftest test-game-map-eid
  (let [conn (helper/create-aruba-conn)
        map-eid (ffirst (d/q '[:find ?m
                               :where
                               [_ :app/game ?g]
                               [?g :game/map ?m]]
                             @conn))]
    (testing "game-map-eid returned by subscription is the same as queried game-map-eid"
      (is (= map-eid @(subs/game-map-eid conn))))))

(deftest test-game-map
  (let [conn (helper/create-aruba-conn)]
    (testing "game map returned by subscription has expected attributes"
      (is (= #{:db/id
               :map/name}
             (into #{} (keys @(subs/game-map conn))))))))

(deftest test-terrains
  (let [conn (helper/create-aruba-conn)
        terrains @(subs/terrains conn)]
    (testing "returns expected number of terrains"
      (is (= 74 (count terrains))))))

(deftest test-current-base-locations
  (let [conn (helper/create-aruba-conn)
        terrains @(subs/terrains conn)]
    (testing "returns the locations of all the current faction's bases"
      (is (= #{[1 2]} @(subs/current-base-locations conn))))))

(deftest test-current-base?
  (let [conn (helper/create-aruba-conn)
        terrains @(subs/terrains conn)]
    (testing "returns true given the coordinates of a base owned by the current faction"
      (is (= true @(subs/current-base? conn 1 2))))
    (testing "returns false given the coordinates of an unowned base"
      (is (= false @(subs/current-base? conn 2 1))))
    (testing "returns false given the coordinates of an enemy base"
      (is (= false @(subs/current-base? conn 7 6))))
    (testing "returns false given the coordinates of a non-base terrain"
      (is (= false @(subs/current-base? conn 7 6))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Factions

(deftest test-current-faction-eid
  (let [conn (helper/create-aruba-conn)
        faction-eid (ffirst (d/q '[:find ?f
                                   :where
                                   [_ :app/game ?g]
                                   [?g :game/current-faction ?f]]
                                 @conn))]
    (testing "returned eid is the same as queried eid"
      (is (= faction-eid @(subs/current-faction-eid conn))))))

(deftest test-current-faction
  (let [conn (helper/create-aruba-conn)]
    (testing "returned faction has expected attributes"
      (is (= #{:db/id
               :faction/color
               :faction/credits
               :faction/next-faction
               :faction/ai
               :faction/order}
             (into #{} (keys @(subs/current-faction conn))))))))

(deftest test-current-unit-count
  (let [conn (helper/create-aruba-conn)]
    (testing "returns the count of units owned by the current faction (blue)"
      (is (= 1 @(subs/current-unit-count conn))))
    (game/end-turn! conn (app/current-game-id @conn))
    (testing "returns the count of units owned by the current faction (red)"
      (is (= 2 @(subs/current-unit-count conn))))))

(deftest test-current-base-count
  (let [conn (helper/create-aruba-conn)]
    (testing "returns the count of bases owned by the current faction"
      (is (= 1 @(subs/current-unit-count conn))))))

(deftest test-unit-eid-at
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns eid for unit at specified coordinates"
      (is (= (:db/id (game/unit-at db game 2 2))
             @(subs/unit-eid-at conn 2 2))))
    (testing "returns nil if there is no unit at the specified coordinates"
      (is (= (:db/id (game/unit-at db game 4 4))
             @(subs/unit-eid-at conn 4 4))))))

(deftest test-current-unit-eid-at
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns eid for current unit at specified coordinates"
      (is (= (:db/id (game/unit-at db game 2 2))
             @(subs/current-unit-eid-at conn 2 2))))
    (testing "returns nil if unit at specified coordinates is an enemy"
      (is (= nil @(subs/current-unit-eid-at conn 7 8))))
    (testing "returns nil if there is no unit at the specified coordinates"
      (is (= nil @(subs/current-unit-eid-at conn 4 4))))))

(deftest test-unit-at?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns true if there is a unit at the specified cordinates"
      (is (= true @(subs/unit-at? conn 2 2))))
    (testing "returns false if there is no unit at the specified coordinates"
      (is (= false @(subs/unit-at? conn 4 4))))))

(deftest test-current-unit-at?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns true for current unit at specified coordinates"
      (is (= true @(subs/current-unit-at? conn 2 2))))
    (testing "returns false if unit at specified coordinates is an enemy"
      (is (= false @(subs/current-unit-at? conn 7 8))))
    (testing "returns false if there is no unit at the specified coordinates"
      (is (= false @(subs/current-unit-at? conn 4 4))))))

(deftest test-unit-at
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returned unit has expected attributes"
      (let [unit @(subs/unit-at conn 2 2)
            unit-keys (keys unit)
            unit-type-keys (keys (:unit/type unit))]
        (is (= #{:db/id
                 :unit/q
                 :unit/r
                 :unit/round-built
                 :unit/move-count
                 :unit/attack-count
                 :unit/count
                 :unit/repaired
                 :unit/capturing
                 :unit/type
                 :faction/_units}
               (into #{} unit-keys)))
        (is (= #{:db/id
                 :unit-type/id
                 :unit-type/name
                 :unit-type/min-range
                 :unit-type/max-range
                 :unit-type/can-capture
                 :unit-type/image}
               (into #{} unit-type-keys)))))))

(deftest test-unit-color-at
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns the color for unit at the specified coordinates"
      (is (= :faction.color/blue @(subs/unit-color-at conn 2 2))))))

(deftest test-unit-type-at
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns the type name for unit at the specified coordinates"
      (is (= :unit-type.id/infantry @(subs/unit-type-at conn 2 2))))))

(deftest test-enemy-locations
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns the locations of all enemy units"
      (is (= #{[7 7] [7 8]} @(subs/enemy-locations conn))))))

(deftest test-enemy-at?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns true if there is an enemy at the specified coordinates"
      (is (= true @(subs/enemy-at? conn 7 7))))
    (testing "returns false if there is not an enemy at the specified coordinates"
      (is (= false @(subs/enemy-at? conn 4 4))))))

(deftest test-enemy-locations-in-range-of
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns enemy locations in range of unit at specified coordinates"
      (is (= #{} @(subs/enemy-locations-in-range-of conn 2 2)))
      (d/transact! conn (game/teleport-tx db game 2 2 6 8))
      (is (= #{[7 8]} @(subs/enemy-locations-in-range-of conn 6 8))))))

(deftest test-any-enemy-in-range-of?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)]
    (testing "returns false if no enemies are in range of unit at specified coordinates"
      (is (= false @(subs/any-enemy-in-range-of? conn 2 2))))
    (testing  "returns true if enemies are in range of unit at specified coordinates"
      (d/transact! conn (game/teleport-tx db game 2 2 6 8))
      (is (= true @(subs/any-enemy-in-range-of? conn 6 8))))))

;; TODO: implement check tests

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Selection

(deftest test-selected?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (d/transact! conn [{:db/id (e app)
                        :app/selected-q 1
                        :app/selected-r 2}])
    (testing "returns true if specified coordinates are selected"
      (is (= true @(subs/selected? conn 1 2))))
    (testing "returns false if specified coordinates are selected"
      (is (= false @(subs/selected? conn 2 2))))))

(deftest test-targeted?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (d/transact! conn [{:db/id (e app)
                        :app/targeted-q 1
                        :app/targeted-r 2}])
    (testing "returns true if specified coordinates are targeted"
      (is (= true @(subs/targeted? conn 1 2))))
    (testing "returns false if specified coordinates are targeted"
      (is (= false @(subs/targeted? conn 2 2))))))

(deftest test-selected-hex
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (testing "returns selected coordinates"
      (is (= nil @(subs/selected-hex conn)))
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 1
                          :app/selected-r 2}])
      (is (= [1 2] @(subs/selected-hex conn)))
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 2
                          :app/selected-r 1}])
      (is (= [2 1] @(subs/selected-hex conn))))))

(deftest test-targeted-hex
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (testing "returns selected coordinates"
      (is (= nil @(subs/targeted-hex conn)))
      (d/transact! conn [{:db/id (e app)
                          :app/targeted-q 1
                          :app/targeted-r 2}])
      (is (= [1 2] @(subs/targeted-hex conn)))
      (d/transact! conn [{:db/id (e app)
                          :app/targeted-q 2
                          :app/targeted-r 1}])
      (is (= [2 1] @(subs/targeted-hex conn))))))

;; TODO: combine with copy in game-test
(def valid-destinations
  #{[0 1] [3 4] [1 2] [1 5] [3 2] [2 4] [4 2] [1 3] [2 3] [3 1]
    [0 2] [3 0] [1 1] [3 3] [1 4] [0 3] [2 1] [4 4] [1 0] [2 0]
    [2 5]})

(deftest test-valid-destinations-for-selected
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (testing "returns valid destinations for selected unit"
      (is (= #{} @(subs/valid-destinations-for-selected conn)))
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 2
                          :app/selected-r 2}])
      (is (= valid-destinations @(subs/valid-destinations-for-selected conn))))))

;; TODO: test valid-destination-for-selected?

(deftest test-selected-can-move-to-targeted?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (testing "returns true if selected unit can move to targeted location"
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 2
                          :app/selected-r 2
                          :app/targeted-q 4
                          :app/targeted-r 4}])
      (is (= true @(subs/selected-can-move-to-targeted? conn))))
    (testing "returns false if selected unit cannot move to targeted location"
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 2
                          :app/selected-r 2
                          :app/targeted-q 5
                          :app/targeted-r 5}])
      (is (= false @(subs/selected-can-move-to-targeted? conn))))))

(deftest test-enemy-in-range-of-selected?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (testing "returns true if selected unit is in range of enemy at specified coordinates"
      (d/transact! conn (conj (game/teleport-tx db game 2 2 6 8)
                              {:db/id (e app)
                               :app/selected-q 6
                               :app/selected-r 8}))
      (is (= true @(subs/enemy-in-range-of-selected? conn 7 8))))
    (testing "returns false if selected unit is not in range of enemy at specified coordinates"
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 6
                          :app/selected-r 8}])
      (is (= false @(subs/enemy-in-range-of-selected? conn 7 7))))))

(deftest test-selected-can-attack-targeted?
  (let [conn (helper/create-aruba-conn)
        db @conn
        game (app/current-game db)
        app (app/root db)]
    (testing "returns true if selected unit can attack targeted unit"
      (d/transact! conn (conj (game/teleport-tx db game 2 2 6 8)
                              {:db/id (e app)
                               :app/selected-q 6
                               :app/selected-r 8
                               :app/targeted-q 7
                               :app/targeted-r 8}))
      (is (= true @(subs/selected-can-attack-targeted? conn))))
    (testing "returns false if selected unit cannot attack targeted unit"
      (d/transact! conn [{:db/id (e app)
                          :app/selected-q 6
                          :app/selected-r 8
                          :app/targeted-q 7
                          :app/targeted-r 7}])
      (is (= false @(subs/selected-can-attack-targeted? conn))))))
