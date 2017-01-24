(ns zetawar.devcards.data-formats
  (:require
   [devcards.core :as dc :include-macros true]
   [integrant.core :as ig]
   [reagent.core :as r]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.subs :as subs]
   [zetawar.system :as system]
   [zetawar.util :refer [breakpoint inspect]]
   [zetawar.views :as views])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(def maps
  {:simple-map
   {:id :simple-map
    :description "Simple Map"
    :terrains
    [;; Row 1
     {:q 0
      :r 0
      :terrain-type :plains}
     {:q 1
      :r 0
      :terrain-type :plains}
     {:q 2
      :r 0
      :terrain-type :plains}]}})

(def scenarios
  {:simple-scenario
   {:id :simple-scenario
    :description "Simple Scenario"
    :ruleset-id :zetawar
    :map-id :simple-map
    :max-count-per-unit 10
    :credits-per-base 100
    :bases
    [{:q 0 :r 0}
     {:q 2 :r 0}]
    :factions
    [{:color :blue
      :credits 100
      :ai false
      :bases [{:q 0 :r 0}]
      :units [{:q 0
               :r 0
               :unit-type :infantry}]}
     {:color :red
      :credits 100
      :ai true
      :bases [{:q 2 :r 0}]
      :units [{:q 2
               :r 0
               :unit-type :infantry}]}]}})

(defcard simple-map-data
  (:simple-map maps))

(defcard simple-scenario-data
  (:simple-scenario scenarios))

(defcard-rg simple-map-and-scenario
  (let [system (ig/init system/game-config)
        game-cfg (:zetawar.system/game system)
        views-cfg (:zetawar.system/game-views system)
        conn (:conn views-cfg)]
    (app/start-new-game! game-cfg
                         data/rulesets
                         maps
                         scenarios
                         :simple-scenario)
    [views/board views-cfg]))
