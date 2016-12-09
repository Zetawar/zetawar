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

;; TODO: remove redundancy between state map and unit/can-capture flag (?)
(def unit-state-maps
  {:move-attack
   {:start-state :start
    :built-state :done
    :states
    {:start {:move-unit    :moved
             :attack-unit  :done
             :repair-unit  :done
             :capture-base :done}
     :moved {:attack-unit  :done
             :capture-base :done}
     :done  {}}}
   :free-attack-twice
   {:start-state :start
    :built-state :done
    :states
    {:start              {:move-unit    :moved-1-attacked-0
                          :attack-unit  :moved-0-attacked-1
                          :repair-unit  :done}
     :moved-0-attacked-1 {:attack-unit  :moved-0-attacked-2
                          :move-unit    :moved-1-attacked-1}
     :moved-0-attacked-2 {:move-unit    :done}
     :moved-1-attacked-0 {:attack-unit  :moved-1-attacked-1}
     :moved-1-attacked-1 {:attack-unit  :done}
     :done               {}}}})

;; TODO: check how capturing armor works in Elite Command
;; TODO: check how repair amount works in Elite Command
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
              :state-map :move-attack
              :image "tilesets/elite-command/units/infantry-COLOR.png"
              :terrain-effects
              {:plains    {:movement-cost 3 :attack-bonus 0 :armor-bonus 0}
               :mountains {:movement-cost 6 :attack-bonus 2 :armor-bonus 5}
               :woods     {:movement-cost 4 :attack-bonus 2 :armor-bonus 3}
               :base      {:movement-cost 2 :attack-bonus 2 :armor-bonus 3}}
              :attack-strengths
              {:personnel 6
               :armored 3}}
   :grenadier {:name "Grenadier"
               :cost 150
               :movement 9
               :can-capture true
               :min-range 1
               :max-range 2
               :armor-type :personnel
               :armor 8
               :capturing-armor 2
               :repair 1
               :state-map :move-attack
               :image "tilesets/elite-command/units/grenadier-COLOR.png"
               :terrain-effects
               {:plains    {:movement-cost 4 :attack-bonus 0  :armor-bonus 0}
                :mountains {:movement-cost 9 :attack-bonus 2  :armor-bonus 5}
                :woods     {:movement-cost 4 :attack-bonus -1 :armor-bonus 3}
                :base      {:movement-cost 3 :attack-bonus -1 :armor-bonus 3}}
               :attack-strengths
               {:personnel 8
                :armored 9}}
   :mortar {:name "Mortar"
            :cost 200
            :movement 9
            :can-capture true
            :min-range 2
            :max-range 3
            :armor-type :personnel
            :armor 6
            :capturing-armor 2
            :repair 1
            :state-map :move-attack
            :image "tilesets/elite-command/units/mortar-COLOR.png"
            :terrain-effects
            {:plains    {:movement-cost 4 :attack-bonus  0 :armor-bonus 0}
             :mountains {:movement-cost 9 :attack-bonus  3 :armor-bonus 5}
             :woods     {:movement-cost 4 :attack-bonus -2 :armor-bonus 3}
             :base      {:movement-cost 3 :attack-bonus -2 :armor-bonus 3}}
            :attack-strengths
            {:personnel 10
             :armored 10}}
   :ranger {:name "Ranger"
            :cost 200
            :movement 9
            :can-capture true
            :min-range 1
            :max-range 1
            :armor-type :personnel
            :armor 6
            :capturing-armor 2
            :repair 1
            :state-map :move-attack
            :image "tilesets/elite-command/units/ranger-COLOR.png"
            :terrain-effects
            {:plains    {:movement-cost 3 :attack-bonus 0 :armor-bonus 0}
             :mountains {:movement-cost 6 :attack-bonus 2 :armor-bonus 5}
             :woods     {:movement-cost 3 :attack-bonus 2 :armor-bonus 4}
             :base      {:movement-cost 2 :attack-bonus 2 :armor-bonus 3}}
            :attack-strengths
            {:personnel 9
             :armored 4}}
   :humvee {:name "Humvee"
            :cost 300
            :movement 15
            :can-capture false
            :min-range 1
            :max-range 1
            :armor-type :armored
            :armor 8
            :capturing-armor 2
            :repair 1
            :state-map :free-attack-twice
            :image "tilesets/elite-command/units/humvee-COLOR.png"
            :terrain-effects
            {:plains    {:movement-cost 3 :attack-bonus  0 :armor-bonus  0}
             :woods     {:movement-cost 6 :attack-bonus -2 :armor-bonus -2}
             :base      {:movement-cost 2 :attack-bonus  0 :armor-bonus  0}}
            :attack-strengths
            {:personnel 9
             :armored 3}}
   :tank {:name "Tank"
          :cost 350
          :movement 12
          :can-capture false
          :min-range 1
          :max-range 1
          :armor-type :armored
          :armor 12
          :capturing-armor 2
          :repair 1
          :state-map :move-attack
          :image "tilesets/elite-command/units/tank-COLOR.png"
          :terrain-effects
          {:plains    {:movement-cost 3 :attack-bonus 0 :armor-bonus  0}
           :woods     {:movement-cost 6 :attack-bonus 0 :armor-bonus -3}
           :base      {:movement-cost 2 :attack-bonus 0 :armor-bonus -2}}
          :attack-strengths
          {:personnel 10
           :armored 10}}
   })

(def maps
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
   :city-sprawl
   {:id :city-sprawl
    :name "City Sprawl"
    :terrains
    [;; Row 1
     {:q 2
      :r 0
      :terrain-type :woods}
     {:q 3
      :r 0
      :terrain-type :woods}
     {:q 4
      :r 0
      :terrain-type :woods}
     {:q 5
      :r 0
      :terrain-type :woods}
     {:q 6
      :r 0
      :terrain-type :woods}
     {:q 10
      :r 0
      :terrain-type :woods}
     {:q 11
      :r 0
      :terrain-type :woods}
     {:q 12
      :r 0
      :terrain-type :woods}
     ;; Row 2
     {:q 1
      :r 1
      :terrain-type :woods}
     {:q 2
      :r 1
      :terrain-type :plains}
     {:q 3
      :r 1
      :terrain-type :plains}
     {:q 4
      :r 1
      :terrain-type :plains}
     {:q 5
      :r 1
      :terrain-type :plains}
     {:q 6
      :r 1
      :terrain-type :woods}
     {:q 8
      :r 1
      :terrain-type :woods}
     {:q 9
      :r 1
      :terrain-type :plains}
     {:q 10
      :r 1
      :terrain-type :plains}
     {:q 11
      :r 1
      :terrain-type :plains}
     {:q 12
      :r 1
      :terrain-type :woods}
     {:q 13
      :r 1
      :terrain-type :woods}
     ;; Row 3
     {:q 1
      :r 2
      :terrain-type :woods}
     {:q 2
      :r 2
      :terrain-type :plains}
     {:q 3
      :r 2
      :terrain-type :mountains}
     ;; Multiplayer base location (unowned)
     {:q 4
      :r 2
      :terrain-type :plains}
     {:q 5
      :r 2
      :terrain-type :plains}
     {:q 6
      :r 2
      :terrain-type :plains}
     {:q 7
      :r 2
      :terrain-type :woods}
     {:q 8
      :r 2
      :terrain-type :woods}
     {:q 9
      :r 2
      :terrain-type :plains}
     {:q 10
      :r 2
      :terrain-type :plains}
     {:q 11
      :r 2
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 12
      :r 2
      :terrain-type :plains}
     {:q 13
      :r 2
      :terrain-type :plains}
     {:q 14
      :r 2
      :terrain-type :woods}
     ;; Row 4
     {:q 0
      :r 3
      :terrain-type :plains}
     {:q 1
      :r 3
      :terrain-type :plains}
     {:q 2
      :r 3
      :terrain-type :mountains}
     ;; Multiplayer base location (red owned)
     {:q 3
      :r 3
      :terrain-type :plains}
     {:q 4
      :r 3
      :terrain-type :mountains}
     ;; Multiplayer base location (unowned)
     {:q 5
      :r 3
      :terrain-type :plains}
     {:q 6
      :r 3
      :terrain-type :woods}
     {:q 7
      :r 3
      :terrain-type :mountains}
     {:q 8
      :r 3
      :terrain-type :mountains}
     {:q 9
      :r 3
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 10
      :r 3
      :terrain-type :plains}
     {:q 11
      :r 3
      :terrain-type :plains} ; should be desert
     {:q 12
      :r 3
      :terrain-type :woods}
     {:q 13
      :r 3
      :terrain-type :plains}
     {:q 14
      :r 3
      :terrain-type :woods}
     ;; Row 5 - TODO: need to finish this row
     {:q 0
      :r 4
      :terrain-type :woods}
     {:q 1
      :r 4
      :terrain-type :plains}
     {:q 2
      :r 4
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
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
      :terrain-type :mountains}
     {:q 7
      :r 4
      :terrain-type :plains}
     {:q 8
      :r 4
      :terrain-type :plains}
     {:q 9
      :r 4
      :terrain-type :woods}
     {:q 10
      :r 4
      :terrain-type :mountains}
     {:q 11
      :r 4
      :terrain-type :plains}
     {:q 12
      :r 4
      :terrain-type :mountains} ; should be desert
     ;; Multiplayer base location (unowned)
     {:q 13
      :r 4
      :terrain-type :plains}
     {:q 14
      :r 4
      :terrain-type :woods}
     ]
    }
   })

(def scenarios
  {:sterlings-aruba-multiplayer
   {:id :sterlings-aruba-multiplayer
    :description "Sterling's Aruba Multiplayer"
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
   :city-sprawl-multiplayer
   {:id :city-sprawl-multiplayer
    :description "City Sprawl Multiplayer"
    :map-id :city-sprawl
    :max-unit-count 10
    :credits-per-base 50
    :bases
    [{:q 4  :r 2}
     {:q 12 :r 2}
     {:q 3  :r 3}
     {:q 5  :r 3}
     {:q 10 :r 3}
     {:q 2  :r 4}
     {:q 4  :r 4}]
    :factions
    [{:color :red
      :credits 300
      :ai false
      :bases [{:q 3 :r 3}]
      :units [{:q 2 :r 2
               :unit-type :infantry}]}
     {:color :blue
      :credits 300
      :ai true
      :bases [{:q 7 :r 6}]
      :units [{:q 12 :r 3
               :unit-type :infantry}
              ]}
     ]
    }
   }
  )
