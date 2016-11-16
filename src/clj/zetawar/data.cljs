(ns zetawar.data)

(def terrains
  {:plains        {:name "Plains"
                   :image "tilesets/elite-command/terrains/plains.png"}
   :mountains     {:name "Mountains"
                   :image "tilesets/elite-command/terrains/mountains.png"}
   :woods         {:name "Woods"
                   :image "tilesets/elite-command/terrains/woods.png"}
   :base          {:name "Base"
                   :image "tilesets/elite-command/terrains/base-COLOR.png"}
   :shallow-water {:name "Shallow Water"
                   :image "tilesets/elite-command/terrains/shallow-water.png"}
   :deep-water    {:name "Deep Water"
                   :image "tilesets/elite-command/terrains/deep-water.png"}})

(def unit-state-maps
  {:basic
   {:start-state :start
    :built-state :done
    :states
    {:start      {:move-unit    :moved-once
                  :attack-unit  :done
                  :repair-unit  :done
                  :capture-base :done}
     :moved-once {:attack-unit  :done
                  :capture-base :done}
     :done       {}}}})

(def units
  {:infantry {:name "Infantry"
              :cost 75
              :movement 9
              :can-capture true
              :min-range 1
              :max-range 1
              :armor-type :personnel
              :armor 6
              :capturing-armor 2
              :repair 1
              :state-map :basic
              :image "tilesets/elite-command/units/infantry-COLOR.png"
              :terrain-effects
              {:plains    {:movement-cost 3 :attack-bonus 0 :armor-bonus 0}
               :mountains {:movement-cost 6 :attack-bonus 2 :armor-bonus 5}
               :woods     {:movement-cost 4 :attack-bonus 2 :armor-bonus 3}
               :base      {:movement-cost 2 :attack-bonus 2 :armor-bonus 3}}
              :attack-strengths
              {:personnel 6}}})

(def specs-tx
  [;; terrain types
   {:db/id -101
    :terrain-type/id :terrain-type.id/base
    :terrain-type/name "base"
    :terrain-type/image "tilesets/elite-command/terrains/base-COLOR.png"}
   {:db/id -102
    :terrain-type/id :terrain-type.id/woods
    :terrain-type/name "woods"
    :terrain-type/image "tilesets/elite-command/terrains/woods.png"}
   {:db/id -103
    :terrain-type/id :terrain-type.id/mountains
    :terrain-type/name "mountains"
    :terrain-type/image "tilesets/elite-command/terrains/mountains.png"}
   {:db/id -104
    :terrain-type/id :terrain-type.id/plains
    :terrain-type/name "plains"
    :terrain-type/image "tilesets/elite-command/terrains/plains.png"}
   {:db/id -105
    :terrain-type/id :terrain-type.id/deep-water
    :terrain-type/name "deep water"
    :terrain-type/image "tilesets/elite-command/terrains/deep-water.png"}

   ;; Unit Types
   {:db/id -201
    :unit-type/id :unit-type.id/infantry
    :unit-type/armor-type :unit-type.armor-type/personnel
    :unit-type/name "Infantry"
    :unit-type/cost 75
    :unit-type/can-capture true
    :unit-type/movement 9
    :unit-type/min-range 1
    :unit-type/max-range 1
    :unit-type/armor 6
    :unit-type/capturing-armor 2
    :unit-type/repair 1
    :unit-type/image "tilesets/elite-command/units/infantry-COLOR.png"}

   ;; Attack
   {:db/id -301
    :unit-strength/unit-type -201 ;; Infantry
    :unit-strength/armor-type :unit-type.armor-type/personnel
    :unit-strength/attack 6}

   ;; Base effects
   {:db/id -401
    :terrain-effect/terrain-type -101 ;; TODO: can eids be replaced by id lookups here?
    :terrain-effect/unit-type -201
    :terrain-effect/attack-bonus 2
    :terrain-effect/armor-bonus 2
    :terrain-effect/movement-cost 3}

   ;; Woods effects
   {:db/id -402
    :terrain-effect/terrain-type -102
    :terrain-effect/unit-type -201
    :terrain-effect/attack-bonus 2
    :terrain-effect/armor-bonus 3
    :terrain-effect/movement-cost 4}

   ;; Mountain effects
   {:db/id -403
    :terrain-effect/terrain-type -103
    :terrain-effect/unit-type -201
    :terrain-effect/attack-bonus 2
    :terrain-effect/armor-bonus 4
    :terrain-effect/movement-cost 6}

   ;; Plain effects
   {:db/id -404
    :terrain-effect/terrain-type -104
    :terrain-effect/unit-type -201
    :terrain-effect/attack-bonus 0
    :terrain-effect/armor-bonus 0
    :terrain-effect/movement-cost 3}
   ])


(def map-definitions
  {:sterlings-aruba
   {:id :sterlings-aruba
    :name "Sterling's Aruba"
    :terrains
    [;; Row 1
     {:q 1
      :r 0
      :terrain-type :plains}
     {:q 2
      :r 0
      :terrain-type :plains}
     {:q 3
      :r 0
      :terrain-type :mountains}
     {:q 4
      :r 0
      :terrain-type :deep-water}
     {:q 5
      :r 0
      :terrain-type :deep-water}
     {:q 6
      :r 0
      :terrain-type :deep-water}
     ;; Row 2
     {:q 0
      :r 1
      :terrain-type :plains}
     {:q 1
      :r 1
      :terrain-type :mountains}
     ;; Multiplayer base location
     {:q 2
      :r 1
      :terrain-type :plains}
     {:q 3
      :r 1
      :terrain-type :plains}
     {:q 4
      :r 1
      :terrain-type :mountains}
     {:q 5
      :r 1
      :terrain-type :woods}
     {:q 6
      :r 1
      :terrain-type :deep-water}
     {:q 7
      :r 1
      :terrain-type :deep-water}
     ;; Row 3
     {:q 0
      :r 2
      :terrain-type :mountains}
     ;; Multiplayer base location
     {:q 1
      :r 2
      :terrain-type :plains}
     {:q 2
      :r 2
      :terrain-type :plains}
     {:q 3
      :r 2
      :terrain-type :plains}
     {:q 4
      :r 2
      :terrain-type :plains}
     {:q 5
      :r 2
      :terrain-type :woods}
     {:q 6
      :r 2
      :terrain-type :mountains}
     {:q 7
      :r 2
      :terrain-type :mountains}
     {:q 8
      :r 2
      :terrain-type :deep-water}
     ;; Row 4
     {:q 0
      :r 3
      :terrain-type :plains}
     {:q 1
      :r 3
      :terrain-type :plains}
     {:q 2
      :r 3
      :terrain-type :plains}
     {:q 3
      :r 3
      :terrain-type :plains}
     {:q 4
      :r 3
      :terrain-type :woods}
     {:q 5
      :r 3
      :terrain-type :plains}
     {:q 6
      :r 3
      :terrain-type :plains}
     {:q 7
      :r 3
      :terrain-type :mountains}
     {:q 8
      :r 3
      :terrain-type :deep-water}
     ;; Row 5
     {:q 0
      :r 4
      :terrain-type :deep-water}
     {:q 1
      :r 4
      :terrain-type :mountains}
     {:q 2
      :r 4
      :terrain-type :plains}
     {:q 3
      :r 4
      :terrain-type :plains}
     {:q 4
      :r 4
      :terrain-type :plains}
     {:q 5
      :r 4
      :terrain-type :plains}
     {:q 6
      :r 4
      :terrain-type :plains}
     {:q 7
      :r 4
      :terrain-type :plains}
     {:q 8
      :r 4
      :terrain-type :deep-water}
     ;; Row 6
     {:q 0
      :r 5
      :terrain-type :deep-water}
     {:q 1
      :r 5
      :terrain-type :plains}
     {:q 2
      :r 5
      :terrain-type :plains}
     {:q 3
      :r 5
      :terrain-type :woods}
     {:q 4
      :r 5
      :terrain-type :plains}
     {:q 5
      :r 5
      :terrain-type :plains}
     {:q 6
      :r 5
      :terrain-type :plains}
     {:q 7
      :r 5
      :terrain-type :plains}
     {:q 8
      :r 5
      :terrain-type :deep-water}
     ;; Row 7
     {:q 0
      :r 6
      :terrain-type :deep-water}
     {:q 1
      :r 6
      :terrain-type :mountains}
     {:q 2
      :r 6
      :terrain-type :plains}
     {:q 3
      :r 6
      :terrain-type :woods}
     {:q 4
      :r 6
      :terrain-type :plains}
     {:q 5
      :r 6
      :terrain-type :plains}
     {:q 6
      :r 6
      :terrain-type :plains}
     ;; Multiplayer base location
     {:q 7
      :r 6
      :terrain-type :plains}
     {:q 8
      :r 6
      :terrain-type :mountains}
     ;; Row 8
     {:q 0
      :r 7
      :terrain-type :deep-water}
     {:q 1
      :r 7
      :terrain-type :mountains}
     {:q 2
      :r 7
      :terrain-type :woods}
     {:q 3
      :r 7
      :terrain-type :mountains}
     {:q 4
      :r 7
      :terrain-type :plains}
     ;; Multiplayer base location
     {:q 5
      :r 7
      :terrain-type :plains}
     {:q 6
      :r 7
      :terrain-type :mountains}
     {:q 7
      :r 7
      :terrain-type :plains}
     ;; Row 9
     {:q 1
      :r 8
      :terrain-type :deep-water}
     {:q 2
      :r 8
      :terrain-type :deep-water}
     {:q 3
      :r 8
      :terrain-type :deep-water}
     {:q 4
      :r 8
      :terrain-type :deep-water}
     {:q 5
      :r 8
      :terrain-type :mountains}
     {:q 6
      :r 8
      :terrain-type :plains}
     {:q 7
      :r 8
      :terrain-type :plains}
    ]
   }
  })

(def scenario-definitions
  {:sterlings-aruba-multiplayer
   {:id :sterlings-aruba-multiplayer
    :map-id :sterlings-aruba
    :max-unit-count 10
    :credits-per-base 100
    :bases
    [{:q 1 :r 2}
     {:q 2 :r 1}
     {:q 5 :r 7}
     {:q 7 :r 6}]
    :factions
    [{:color :blue
      :credits 300
      :ai false
      :bases [{:q 1 :r 2}]
      :units [{:q 2
               :r 2
               :unit-type :infantry}]}
     {:color :red
      :credits 300
      :ai true
      :bases [{:q 7 :r 6}]
      :units [{:q 7
               :r 7
               :unit-type :infantry}
              {:q 7
               :r 8
               :unit-type :infantry}]}]
    }
   }
  )
