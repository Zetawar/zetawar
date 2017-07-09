(ns zetawar.game-test
  (:require
   [cljs.test :refer-macros [testing is async use-fixtures]]
   [datascript.core :as d]
   [devcards.core :as dc :refer-macros [defcard deftest]]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.db :as db :refer [e qe]]
   [zetawar.game :as game]
   [zetawar.test-helper :as helper]
   [zetawar.util :refer [breakpoint inspect]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Util

(deftest test-game-pos-idx
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (integer? (game/game-pos-idx game 1 2)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Game

(deftest test-game-by-id
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (= game (game/game-by-id db (:game/id game))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Terrain

(deftest test-terrain?
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)
        terrain (game/terrain-at db game 1 0)]
    (testing "a unit is not a terrain"
      (is (not (game/terrain? unit))))
    (testing "a terrain is a terrain"
      (is (game/terrain? terrain)))))

(deftest test-terrain-hex
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        terrain (game/terrain-at db game 1 0)]
    (is (= [1 0] (game/terrain-hex terrain)))))

(deftest test-terrain-at
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (nil? (game/base-at db game 99 99)))
    (is (game/terrain? (game/base-at db game 1 2)))))

(deftest test-checked-terrain-at
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (thrown? ExceptionInfo (game/checked-terrain-at db game 99 99)))
    (is (game/terrain? (game/checked-terrain-at db game 1 2)))))

(deftest test-base-at
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (nil? (game/base-at db game 1 0)))
    (is (game/base? (game/base-at db game 1 2)))))

(deftest test-checked-base-at
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (thrown? ExceptionInfo (game/checked-base-at db game 1 0)))
    (is (game/base? (game/checked-base-at db game 1 2)))))

(deftest test-check-base-current
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        current-base (game/base-at db game 1 2)
        unowned-base (game/base-at db game 2 1)
        enemy-base (game/base-at db game 7 6)]
    (is (nil? (game/check-base-current db game current-base)))
    (is (thrown? ExceptionInfo (game/check-base-current db game unowned-base)))
    (is (thrown? ExceptionInfo (game/check-base-current db game enemy-base)))))

(deftest test-check-current-base?
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        current-base (game/base-at db game 1 2)
        unowned-base (game/base-at db game 2 1)
        enemy-base (game/base-at db game 7 6)]
    (is (game/current-base? db game current-base))
    (is (not (game/current-base? db game unowned-base)))
    (is (not (game/current-base? db game enemy-base)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Units

(deftest test-unit?
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)
        terrain (game/terrain-at db game 1 0)]
    (is (not (game/unit? terrain)))
    (is (game/unit? unit))))

(deftest test-unit-hex
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (is (= [2 2] (game/unit-hex unit)))))

(deftest test-unit-at
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (nil? (game/unit-at db game 99 99)))
    (is (game/unit? (game/unit-at db game 2 2)))))

(deftest test-checked-unit-at
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (is (thrown? ExceptionInfo (game/checked-unit-at db game 99 99)))
    (is (game/unit? (game/checked-unit-at db game 2 2)))))

(deftest test-unit-faction
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (is (= :faction.color/blue
           (:faction/color (game/unit-faction db unit))))))

(deftest test-check-unit-current
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        current-unit (game/unit-at db game 2 2)
        enemy-unit (game/unit-at db game 7 7) ]
    (is (= nil (game/check-unit-current db game current-unit)))
    (is (thrown? ExceptionInfo (game/check-unit-current db game enemy-unit)))))

(deftest test-unit-current?
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        current-unit (game/unit-at db game 2 2)
        enemy-unit (game/unit-at db game 7 7) ]
    (is (game/unit-current? db game current-unit))
    (is (not (game/unit-current? db game enemy-unit)))))

(deftest test-on-base?
  (let [conn (helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game @conn) ]
    (d/transact conn (game/teleport-tx @conn game 2 2 1 2))
    (let [db @conn]
      (is (game/on-base? db game (game/unit-at db game 1 2)))
      (is (not (game/on-base? db game (game/unit-at db game 7 7)))))))

(deftest test-on-capturable-base?
  (let [conn (helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game @conn) ]
    (d/transact conn (game/teleport-tx @conn game 2 2 1 2))
    (is (not (game/on-capturable-base? @conn game (game/unit-at @conn game 1 2))))
    (d/transact conn (game/teleport-tx @conn game 1 2 2 1))
    (is (game/on-capturable-base? @conn game (game/unit-at @conn game 2 1)))
    (d/transact conn (game/teleport-tx @conn game 2 1 7 6))
    (is (game/on-capturable-base? @conn game (game/unit-at @conn game 7 6)))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Movement

(def valid-destinations
  #{[0 1] [3 4] [1 2] [1 5] [3 2] [2 4] [4 2] [1 3] [2 3] [3 1]
    [0 2] [3 0] [1 1] [3 3] [1 4] [0 3] [2 1] [4 4] [1 0] [2 0]
    [2 5]})

(deftest test-valid-moves
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (is (= #{:to :from :cost :path}
           (into #{} (-> (game/valid-moves db game unit)
                         first
                         keys))))
    (is (= valid-destinations
           (into #{}
                 (map :to)
                 (game/valid-moves db game unit))))))

(deftest test-valid-destinations
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (is (= valid-destinations (game/valid-destinations db game unit)))))

;; TODO: add test for check-valid-destination

(deftest test-valid-destination?
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (is (= (count valid-destinations)
           (count (filter #(apply game/valid-destination? db game unit %)
                          valid-destinations))))
    (is (= false (game/valid-destination? db game unit 6 6)))))

(deftest test-can-move-fns
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]

    ;; TODO: update for state machine changes

    #_(testing "units that have not yet performed any actions can move"
        (is (= nil (game/check-can-move db game unit)))
        (is (= true (game/can-move? db game unit))))
    #_(testing "newly built units cannot move"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/round-built 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-move db' game unit')))
          (is (= false (game/can-move? db' game unit')))))
    #_(testing "moved units cannot move again"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/move-count 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-move db' game unit')))
          (is (= false (game/can-move? db' game unit')))))
    #_(testing "units that have already attacked cannot move"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/attack-count 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-move db' game unit')))
          (is (= false (game/can-move? db' game unit')))))
    #_(testing "capturing units cannot move"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/capturing true]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-move db' game unit')))
          (is (= false (game/can-move? db' game unit')))))
    #_(testing "repaired units cannot move"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/repaired true]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-move db' game unit')))
          (is (= false (game/can-move? db' game unit')))))

    ))

(deftest test-teleport-tx
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (let [db' (d/db-with db (game/teleport-tx db game 2 2 4 4))
          teleported-unit (game/unit-at db' game 4 4)]
      (is (= (e unit) (e teleported-unit)))
      (is (= 0 (:unit/move-count teleported-unit))))

    ;; TODO: test moving to an invalid destination

    ))

(deftest test-move-tx
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (testing "moving to a valid destination"
      (let [db' (d/db-with db (game/move-tx db game 2 2 4 4))
            moved-unit (game/unit-at db' game 4 4)]
        (is (= (e unit) (e moved-unit)))
        (is (= 1 (:unit/move-count moved-unit)))))

    ;; TODO: test moving to an invalid destination

    ))

(deftest test-move!
  (let [conn (helper/create-scenario-conn :sterlings-aruba-multiplayer)
        db @conn
        game (app/current-game db)
        game-id (app/current-game-id db)
        unit (game/unit-at db game 2 2)]
    (game/move! conn game-id 2 2 4 4)
    (is (= (e unit) (e (game/unit-at @conn game 4 4))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Attack

(deftest test-can-attack-fns
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]

    ;; TODO: update for state machine changes

    #_(testing "units that have not yet performed any actions can attack"
        (is (= nil (game/check-can-attack db game unit)))
        (is (= true (game/can-attack? db game unit))))
    #_(testing "newly built units cannot attack"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/round-built 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-attack db' game unit')))
          (is (= false (game/can-attack? db' game unit')))))
    #_(testing "moved units can attack"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/move-count 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (= nil (game/check-can-attack db' game unit')))
          (is (= true (game/can-attack? db' game unit')))))
    #_(testing "units that have already attacked cannot attack again"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/attack-count 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-attack db' game unit')))
          (is (= false (game/can-attack? db' game unit')))))
    #_(testing "capturing units cannot attack"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/capturing true]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-attack db' game unit')))
          (is (= false (game/can-attack? db' game unit')))))
    #_(testing "repaired units cannot attack"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/repaired true]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-attack db' game unit')))
          (is (= false (game/can-attack? db' game unit')))))

    ))

(deftest test-in-range-fns
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)]
    (testing "can attack units that are in range"
      (let [db' (d/db-with db (game/teleport-tx db game 7 8 3 2))
            attacker (game/unit-at db' game 2 2)
            defender (game/unit-at db' game 3 2)]
        (is (= nil (game/check-in-range db' attacker defender)))
        (is (= true (game/in-range? db' attacker defender)))))
    (testing "cannot attack units that are out of range"
      (let [attacker (game/unit-at db game 2 2)
            defender (game/unit-at db game 7 8)]
        (is (thrown? ExceptionInfo (game/check-in-range db attacker defender)))
        (is (= false (game/in-range? db attacker defender)))))))

(deftest test-attack-tx
  (let [conn (helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game @conn)
        db (d/db-with @conn (game/teleport-tx @conn game 7 8 3 2))]
    (testing "attacking unit in range"
      (let [db' (d/db-with db (game/attack-tx db game 2 2 3 2))
            attacker (game/unit-at db' game 2 2)
            defender (game/unit-at db' game 3 2)]
        (is (< (:unit/count attacker) 10))
        (is (= (:unit/attack-count attacker) 1))
        (is (< (:unit/count defender) 10))
        (is (= (:unit/attack-count defender) 0))))))

;; TODO: test attack!

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Repair

(deftest test-repair-checking
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)]
    (testing "undamaged units cannot be repaired"
      (is (thrown? ExceptionInfo (game/check-can-repair db game unit)))
      (is (= false (game/can-repair? db game unit))))

    ;; TODO: update for state machine changes

    #_(testing "damaged units that have not performed any actions can be repaired"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/count 9]])
              unit' (game/unit-at db' game 2 2)]
          (is (= nil (game/check-can-repair db' game unit')))
          (is (= true (game/can-repair? db' game unit')))))
    #_(testing "newly built units cannot be repaired"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/round-built 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-repair db' game unit')))
          (is (= false (game/can-repair? db' game unit')))))
    #_(testing "moved units cannot be repaired"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/move-count 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-repair db' game unit')))
          (is (= false (game/can-repair? db' game unit')))))
    #_(testing "units that have already attacked cannot be repaired"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/attack-count 1]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-repair db' game unit')))
          (is (= false (game/can-repair? db' game unit')))))
    #_(testing "capturing units cannot be repaired"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/capturing true]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-repair db' game unit')))
          (is (= false (game/can-repair? db' game unit')))))
    #_(testing "already repaired units cannot be repaired"
        (let [db' (d/db-with db [[:db/add (e unit) :unit/repaired true]])
              unit' (game/unit-at db' game 2 2)]
          (is (thrown? ExceptionInfo (game/check-can-repair db' game unit')))
          (is (= false (game/can-repair? db' game unit')))))

    ))

(deftest test-repair-tx
  (let [conn (helper/create-scenario-conn :sterlings-aruba-multiplayer)
        db @conn
        game (app/current-game db)
        unit (game/unit-at db game 2 2)
        damaged-db (-> db
                       (d/db-with (game/teleport-tx db game 2 2 1 2))
                       (d/db-with [[:db/add (e unit) :unit/count 9]]))
        damaged-unit (game/unit-at damaged-db game 1 2)]
    (testing "repairing damaged unit"
      (let [repaired-db (d/db-with damaged-db (game/repair-tx damaged-db game damaged-unit))
            repaired-unit (game/unit-at repaired-db game 1 2)]
        (is (= (:unit/count repaired-unit) 10))))))

;; TODO: test repair!

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Capture

(deftest test-can-capture-fns
  (let [db @(helper/create-scenario-conn :sterlings-aruba-multiplayer)
        game (app/current-game db)
        unit (game/unit-at db game 2 2)
        terrain (game/terrain-at db game 2 2)]
    (testing "unit not on a base cannot capture"
      (is (thrown? ExceptionInfo (game/check-capturable db game unit terrain)))
      (is (= false (game/can-capture? db game unit terrain))))
    (testing "unit on already owned base cannot capture"
      (let [db' (d/db-with db (game/teleport-tx db game 2 2 1 2))
            unit' (game/unit-at db' game 1 2)
            terrain' (game/terrain-at db game 1 2)]
        (is (thrown? ExceptionInfo (game/check-capturable db' game unit' terrain')))
        (is (= false (game/can-capture? db' game unit' terrain')))))

    ;; TODO: update for state machine changes

    #_(testing "unit on unowned base can capture"
        (let [db' (d/db-with db (game/teleport-tx db game 2 2 2 1))
              unit' (game/unit-at db' game 2 1)
              terrain' (game/terrain-at db' game 2 1)]
          (is (= nil (game/check-capturable db' game unit' terrain')))
          (is (= true (game/can-capture? db' game unit' terrain')))))
    #_(testing "unit on enemy base can capture"
        (let [db' (d/db-with db (game/teleport-tx db game 2 2 7 6))
              unit' (game/unit-at db' game 7 6)
              terrain' (game/terrain-at db' game 7 6)]
          (is (= nil (game/check-capturable db' game unit' terrain')))
          (is (= true (game/can-capture? db' game unit' terrain')))))
    #_(testing "units that have attacked cannot capture"
        (let [db' (d/db-with db (concat (game/teleport-tx db game 2 2 2 1)
                                        [[:db/add (e unit) :unit/attack-count 1]]))
              unit' (game/unit-at db' game 2 1)
              terrain' (game/terrain-at db' game 2 1)]
          (is (thrown? ExceptionInfo (game/check-capturable db' game unit' terrain')))
          (is (= false (game/can-capture? db' game unit' terrain')))))
    #_(testing "repaired units cannot capture"
        (let [db' (d/db-with db (concat (game/teleport-tx db game 2 2 2 1)
                                        [[:db/add (e unit) :unit/repaired true]]))
              unit' (game/unit-at db' game 2 1)
              terrain' (game/terrain-at db' game 2 1)]
          (is (thrown? ExceptionInfo (game/check-capturable db' game unit' terrain')))
          (is (= false (game/can-capture? db' game unit' terrain')))))

    (testing "already capturing units cannot capture"
      (let [db' (d/db-with db (concat (game/teleport-tx db game 2 2 2 1)
                                      [[:db/add (e unit) :unit/capturing true]]))
            unit' (game/unit-at db' game 2 1)
            terrain' (game/terrain-at db' game 2 1)]
        (is (thrown? ExceptionInfo (game/check-capturable db' game unit' terrain')))
        (is (= false (game/can-capture? db' game unit' terrain')))))))

(deftest test-capture-tx
  (let [conn (helper/create-scenario-conn :sterlings-aruba-multiplayer)
        db @conn
        game (app/current-game db)
        unit (game/unit-at db game 2 2)
        moved-db (d/db-with db (game/teleport-tx db game 2 2 2 1))
        moved-unit (game/unit-at moved-db game 2 1)]
    (testing "capturing unowned base"
      (let [capturing-db (d/db-with moved-db (game/capture-tx moved-db game moved-unit))
            capturing-unit (game/unit-at capturing-db game 2 1)]
        (is (= (:unit/capturing capturing-unit) true))
        (is (= (:unit/capture-round capturing-unit) 2))))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Build

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; End Turn

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Setup

(comment

  (defcard terrain-types-tx
    (let [terrains-spec (select-keys data/ruleset [:terrains :plains])]
      {:terrains-spec terrains-spec
       :terrain-types-tx (game/terrain-types-tx terrains-spec)}))

  (defcard units-spec-tx
    (let [conn (d/create-conn db/schema)
          units-spec (select-keys data/ruleset [:units :infantry])]
      (d/transact! conn (game/terrain-types-tx (:terains data/ruleset)))
      {:units-spec units-spec
       :unit-types-tx (game/unit-types-tx @conn units-spec)}))

  (deftest test-map-tx
    (let [game-id (random-uuid)]
      (is (= nil (game/map-tx game-id (:sterlings-aruba data/maps))))
      ))

  )
