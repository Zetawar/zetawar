(ns zetawar.data)

(def dicts
  {:en
   {;; General
    :save-button "Save"
    :cancel-button "Cancel"
    :close-button "Close"

    ;; Game links
    :copy-game-url-link "Copy Link"
    :new-game-link "New Game"
    :end-turn-link "End Turn?"

    ;; Game labels
    :credits-label "Credits"
    :round-label "Round"

    ;; Faction configuration
    :configure-faction-tip "Configure faction"
    :configure-faction-title-prefix "Configure faction: "
    :player-type-label "Player type"

    ;; Faction colors
    :red-name "Red"
    :blue-name "Blue"
    :yellow-name "Yellow"
    :pink-name "Pink"
    :green-name "Green"
    :orange-name "Orange"

    ;; New game settings
    :new-game-title "Start a new game"
    :scenario-label "Scenario"
    :start-button "Start"

    ;; Unit and base actions
    :move-unit-button "Move"
    :build-unit-button "Build"
    :attack-unit-button "Attack"
    :repair-unit-button "Repair"
    :field-repair-button "Field Repair"
    :capture-base-button "Capture"

    ;; Building units
    :build-title "Select a unit to build"
    :unit-cost-label "Cost"

    ;; Win dialog
    :win-title "Congratulations! You won!"
    :win-body
    (str "Thanks for playing Zetawar! If you're interested in staying up-to-date "
         "with Zetawar as it develops, please follow "
         "<a href=\"https://twitter.com/ZetawarGame\">ZetawarGame</a> on Twitter.")

    ;; Gameplay tips
    :select-unit-or-base-tip "Select a unit or base."
    :select-target-or-destination-tip "Select a destination or target to move, attack, or repair."
    :multiplayer-tip
    (str "To play multiplayer follow the instructions "
         "<a href=\"https://www.kickstarter.com/projects/311016908/zetawar/posts/1608417\">"
         "here</a>.")
    }})

;; TODO: remove redundant id keys (?)
(def rulesets
  {:zetawar
   {:settings
    {:ranged-attack-bonus   1
     :adjacent-attack-bonus 1
     :flanking-attack-bonus 2
     :opposite-attack-bonus 3
     :stochastic-damage     false
     :self-repair           true
     :move-through-friendly true}

    :terrains
    {:plains        {:description "Plains"
                     :image "tilesets/elite-command/terrains/plains.png"
                     :base-type false}
     :mountains     {:description "Mountains"
                     :image "tilesets/elite-command/terrains/mountains.png"
                     :base-type false}
     :woods         {:description "Woods"
                     :image "tilesets/elite-command/terrains/woods.png"
                     :base-type false}
     :desert        {:description "Desert"
                     :image "tilesets/elite-command/terrains/desert.png"
                     :base-type false}
     :tundra        {:description "Tundra"
                     :image "tilesets/elite-command/terrains/tundra.png"
                     :base-type false}
     :swamp         {:description "Swamp"
                     :image "tilesets/elite-command/terrains/swamp.png"
                     :base-type false}
     :ford          {:description "Ford"
                     :image "tilesets/elite-command/terrains/ford.png"
                     :base-type false}
     :shallow-water {:description "Shallow Water"
                     :image "tilesets/elite-command/terrains/shallow-water.png"
                     :base-type false}
     :deep-water    {:description "Deep Water"
                     :image "tilesets/elite-command/terrains/deep-water.png"
                     :base-type false}
     :base          {:description "Base"
                     :image "tilesets/elite-command/terrains/base-COLOR.png"
                     :base-type true}
     :seaport       {:description "Seaport"
                     :image "tilesets/elite-command/terrains/seaport-COLOR.png"
                     :base-type true}
     :airfield      {:description "Airfield"
                     :image "tilesets/elite-command/terrains/airfield-COLOR.png"
                     :base-type true}}

    :unit-state-maps
    {:move-attack-once
     {:start-state :start
      :built-state :done
      :states
      {:start {:transitions
               {:move-unit         :moved
                :attack-unit       :done
                :repair-unit       :done
                :field-repair-unit :done
                :capture-base      :done}}
       :moved {:transitions
               {:attack-unit       :done
                :capture-base      :done
                :field-repair-unit :done}}
       :done  {}}}

     :move-attack-twice
     {:start-state :start
      :built-state :done
      :states
      {:start              {:transitions
                            {:move-unit    :moved-1-attacked-0
                             :attack-unit  :moved-0-attacked-1
                             :repair-unit  :done
                             :capture-base :done}}
       :moved-0-attacked-1 {:transitions
                            {:attack-unit  :done
                             :capture-base :done}}
       :moved-1-attacked-0 {:transitions
                            {:attack-unit  :moved-1-attacked-1
                             :capture-base :done}}
       :moved-1-attacked-1 {:transitions
                            {:attack-unit  :done
                             :capture-base :done}}
       :done               {}}}

     :free-attack-once
     {:start-state :start
      :built-state :done
      :states
      {:start              {:transitions
                            {:move-unit    :moved-1-attacked-0
                             :attack-unit  :moved-0-attacked-1
                             :repair-unit  :done
                             :capture-base :done}}
       :moved-0-attacked-1 {:transitions
                            {:move-unit    :done
                             :capture-base :done}}
       :moved-1-attacked-0 {:transitions
                            {:attack-unit  :done
                             :capture-base :done}}
       :done               {}}}

     :free-attack-twice
     {:start-state :start
      :built-state :done
      :states
      {:start              {:transitions
                            {:move-unit    :moved-1-attacked-0
                             :attack-unit  :moved-0-attacked-1
                             :repair-unit  :done
                             :capture-base :done}}
       :moved-0-attacked-1 {:transitions
                            {:move-unit    :moved-1-attacked-1
                             :attack-unit  :moved-0-attacked-2
                             :capture-base :done}}
       :moved-0-attacked-2 {:transitions
                            {:move-unit    :done
                             :capture-base :done}}
       :moved-1-attacked-0 {:transitions
                            {:attack-unit  :moved-1-attacked-1
                             :capture-base :done}}
       :moved-1-attacked-1 {:transitions
                            {:attack-unit  :done
                             :capture-base :done}}
       :done               {}}}}

    ;; TODO: check how repair amount works in Elite Command
    ;; TODO: add :buildable-at (or -by?) => {<terrain type ids>...}
    :units
    {:infantry {:description "Infantry"
                :cost 75
                :movement 9
                :can-capture true
                :can-repair #{}
                :min-range 1
                :max-range 1
                :armor-type :personnel
                :armor 6
                :capturing-armor 4
                :repair 1
                :state-map :move-attack-once
                :buildable-at #{:base}
                :image "tilesets/elite-command/units/infantry-COLOR.png"
                :terrain-effects
                {:plains    {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
                 :mountains {:movement-cost 6 :armor-bonus  5 :attack-bonus  2}
                 :woods     {:movement-cost 4 :armor-bonus  3 :attack-bonus  2}
                 :desert    {:movement-cost 4 :armor-bonus -1 :attack-bonus -1}
                 :tundra    {:movement-cost 4 :armor-bonus -1 :attack-bonus -1}
                 :swamp     {:movement-cost 6 :armor-bonus -2 :attack-bonus -2}
                 :ford      {:movement-cost 5 :armor-bonus -1 :attack-bonus -1}
                 :base      {:movement-cost 2 :armor-bonus  3 :attack-bonus  2}
                 :seaport   {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}
                 :airfield  {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}}
                :attack-strengths
                {:personnel 6
                 :armored   3
                 :naval     4
                 :air       0}
                :zoc
                [:personnel :armored]}
     :grenadier {:description "Grenadier"
                 :cost 150
                 :movement 9
                 :can-capture true
                 :can-repair #{}
                 :min-range 1
                 :max-range 2
                 :armor-type :personnel
                 :armor 8
                 :capturing-armor 6
                 :repair 1
                 :state-map :move-attack-once
                 :buildable-at #{:base}
                 :image "tilesets/elite-command/units/grenadier-COLOR.png"
                 :terrain-effects
                 {:plains    {:movement-cost 4 :armor-bonus  0 :attack-bonus  0}
                  :mountains {:movement-cost 9 :armor-bonus  5 :attack-bonus  2}
                  :woods     {:movement-cost 4 :armor-bonus  3 :attack-bonus -1}
                  :desert    {:movement-cost 5 :armor-bonus -1 :attack-bonus  0}
                  :tundra    {:movement-cost 5 :armor-bonus -1 :attack-bonus  0}
                  :swamp     {:movement-cost 9 :armor-bonus -2 :attack-bonus -2}
                  :ford      {:movement-cost 9 :armor-bonus -1 :attack-bonus -1}
                  :base      {:movement-cost 3 :armor-bonus  3 :attack-bonus -1}
                  :seaport   {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
                  :airfield  {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}}
                 :attack-strengths
                 {:personnel 8
                  :armored   9
                  :naval     6
                  :air       3}
                 :zoc
                 [:personnel :armored :naval]}
     :mortar {:description "Mortar"
              :cost 200
              :movement 9
              :can-capture true
              :can-repair #{}
              :min-range 2
              :max-range 3
              :armor-type :personnel
              :armor 6
              :capturing-armor 4
              :repair 1
              :state-map :move-attack-once
              :buildable-at #{:base}
              :image "tilesets/elite-command/units/mortar-COLOR.png"
              :terrain-effects
              {:plains    {:movement-cost 4 :armor-bonus  0 :attack-bonus  0}
               :mountains {:movement-cost 9 :armor-bonus  5 :attack-bonus  3}
               :woods     {:movement-cost 4 :armor-bonus  3 :attack-bonus -2}
               :desert    {:movement-cost 5 :armor-bonus -1 :attack-bonus  0}
               :tundra    {:movement-cost 5 :armor-bonus -1 :attack-bonus -1}
               :swamp     {:movement-cost 9 :armor-bonus -2 :attack-bonus -2}
               :ford      {:movement-cost 9 :armor-bonus -1 :attack-bonus -2}
               :base      {:movement-cost 3 :armor-bonus  3 :attack-bonus -2}
               :seaport   {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
               :airfield  {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}}
              :attack-strengths
              {:personnel 10
               :armored   10
               :naval     10
               :air       0}}
     :ranger {:description "Ranger"
              :cost 200
              :movement 9
              :can-capture true
              :can-repair #{}
              :min-range 1
              :max-range 1
              :armor-type :personnel
              :armor 9
              :capturing-armor 7
              :repair 1
              :state-map :move-attack-once
              :buildable-at #{:base}
              :image "tilesets/elite-command/units/ranger-COLOR.png"
              :terrain-effects
              {:plains        {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
               :mountains     {:movement-cost 6 :armor-bonus  5 :attack-bonus  2}
               :woods         {:movement-cost 3 :armor-bonus  4 :attack-bonus  2}
               :desert        {:movement-cost 3 :armor-bonus -1 :attack-bonus -1}
               :tundra        {:movement-cost 3 :armor-bonus -1 :attack-bonus -1}
               :swamp         {:movement-cost 3 :armor-bonus -2 :attack-bonus -2}
               :ford          {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
               :shallow-water {:movement-cost 6 :armor-bonus -2 :attack-bonus -2}
               :base          {:movement-cost 2 :armor-bonus  3 :attack-bonus  2}
               :seaport       {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}
               :airfield      {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}}
              :attack-strengths
              {:personnel 9
               :armored   4
               :naval     6
               :air       0}
              :zoc
              [:personnel :armored]}
     :medic {:description "Medic"
             :cost 100
             :movement 9
             :can-capture true
             :can-repair #{:personnel}
             :min-range 1
             :max-range 1
             :armor-type :personnel
             :armor 6
             :capturing-armor 4
             :repair 1
             :state-map :move-attack-once
             :buildable-at #{:base}
             :image "tilesets/elite-command/units/medic-COLOR.png"
             :terrain-effects
             {:plains    {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
              :mountains {:movement-cost 6 :armor-bonus  5 :attack-bonus  2}
              :woods     {:movement-cost 4 :armor-bonus  3 :attack-bonus  2}
              :desert    {:movement-cost 4 :armor-bonus -1 :attack-bonus -1}
              :tundra    {:movement-cost 4 :armor-bonus -1 :attack-bonus -1}
              :swamp     {:movement-cost 6 :armor-bonus -2 :attack-bonus -2}
              :ford      {:movement-cost 5 :armor-bonus -1 :attack-bonus -1}
              :base      {:movement-cost 2 :armor-bonus  3 :attack-bonus  2}
              :seaport   {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}
              :airfield  {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}}
            :attack-strengths
            {:personnel 5
             :armored   2
             :naval     3
             :air       0}
            :zoc
            [:personnel :armored]}
    :engineer {:description "Engineer"
               :cost 200
               :movement 9
               :can-capture true
               :can-repair #{:armored}
               :min-range 1
               :max-range 1
               :armor-type :personnel
               :armor 6
               :capturing-armor 4
               :repair 1
               :state-map :move-attack-once
               :buildable-at #{:base}
               :image "tilesets/elite-command/units/engineer-COLOR.png"
               :terrain-effects
               {:plains        {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
                :mountains     {:movement-cost 6 :armor-bonus  5 :attack-bonus  2}
                :woods         {:movement-cost 4 :armor-bonus  3 :attack-bonus  2}
                :desert        {:movement-cost 4 :armor-bonus -1 :attack-bonus -1}
                :tundra        {:movement-cost 4 :armor-bonus -1 :attack-bonus -1}
                :swamp         {:movement-cost 6 :armor-bonus -2 :attack-bonus -2}
                :ford          {:movement-cost 5 :armor-bonus -1 :attack-bonus -1}
                :shallow-water {:movement-cost 7 :armor-bonus -2 :attack-bonus -2}
                :base          {:movement-cost 2 :armor-bonus  3 :attack-bonus  2}
                :seaport       {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}
                :airfield      {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}}
              :attack-strengths
              {:personnel 5
               :armored   2
               :naval     3
               :air       0}
              :zoc
              [:personnel :armored]}
     ;; Armored
     :humvee {:description "Humvee"
              :cost 300
              :movement 15
              :can-capture false
              :can-repair #{}
              :min-range 1
              :max-range 1
              :armor-type :armored
              :armor 8
              :repair 1
              :state-map :free-attack-twice
              :buildable-at #{:base}
              :image "tilesets/elite-command/units/humvee-COLOR.png"
              :terrain-effects
              {:plains   {:movement-cost 3  :armor-bonus  0 :attack-bonus  0}
               :woods    {:movement-cost 6  :armor-bonus -2 :attack-bonus -2}
               :desert   {:movement-cost 3  :armor-bonus  0 :attack-bonus  0}
               :tundra   {:movement-cost 6  :armor-bonus  0 :attack-bonus  0}
               :swamp    {:movement-cost 12 :armor-bonus -3 :attack-bonus -3}
               :ford     {:movement-cost 12 :armor-bonus -1 :attack-bonus -1}
               :base     {:movement-cost 2  :armor-bonus  0 :attack-bonus  0}
               :seaport  {:movement-cost 2  :armor-bonus  0 :attack-bonus  0}
               :airfield {:movement-cost 2  :armor-bonus  0 :attack-bonus  0}}
              :attack-strengths
              {:personnel 9
               :armored   3
               :naval     6
               :air       0}
              :zoc
              [:personnel :armored]}
     :tank {:description "Tank"
            :cost 350
            :movement 12
            :can-capture false
            :can-repair #{}
            :min-range 1
            :max-range 1
            :armor-type :armored
            :armor 12
            :repair 1
            :state-map :move-attack-once
            :buildable-at #{:base}
            :image "tilesets/elite-command/units/tank-COLOR.png"
            :terrain-effects
            {:plains   {:movement-cost 3 :armor-bonus  0 :attack-bonus 0}
             :woods    {:movement-cost 6 :armor-bonus -3 :attack-bonus 0}
             :desert   {:movement-cost 4 :armor-bonus  0 :attack-bonus 0}
             :tundra   {:movement-cost 5 :armor-bonus  0 :attack-bonus 0}
             :swamp    {:movement-cost 8 :armor-bonus -4 :attack-bonus 0}
             :ford     {:movement-cost 8 :armor-bonus  0 :attack-bonus 0}
             :base     {:movement-cost 2 :armor-bonus -2 :attack-bonus 0}
             :seaport  {:movement-cost 2 :armor-bonus  0 :attack-bonus 0}
             :airfield {:movement-cost 2 :armor-bonus  0 :attack-bonus 0}}
            :attack-strengths
            {:personnel 10
             :armored   10
             :naval     9
             :air       0}
            :zoc
            [:personnel :armored]}
     :mobile-flak {:description "Mobile Flak"
                   :cost 350
                   :movement 9
                   :can-capture false
                   :can-repair #{}
                   :min-range 1
                   :max-range 3
                   :armor-type :armored
                   :armor 8
                   :repair 1
                   :state-map :move-attack
                   :buildable-at #{:base :airfield}
                   :image "tilesets/elite-command/units/mobileflak-COLOR.png"
                   :terrain-effects
                   {:plains   {:movement-cost 3 :armor-bonus  0 :attack-bonus  0}
                    :woods    {:movement-cost 6 :armor-bonus -2 :attack-bonus -3}
                    :desert   {:movement-cost 6 :armor-bonus  0 :attack-bonus  0}
                    :tundra   {:movement-cost 6 :armor-bonus -1 :attack-bonus  0}
                    :swamp    {:movement-cost 6 :armor-bonus -3 :attack-bonus -2}
                    :ford     {:movement-cost 6 :armor-bonus -2 :attack-bonus -1}
                    :base     {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}
                    :seaport  {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}
                    :airfield {:movement-cost 2 :armor-bonus  0 :attack-bonus  0}}
                   :attack-strengths
                   {:personnel 0
                    :armored   0
                    :naval     0
                    :air       14}
                   :zoc
                   [:air]}
     ;; Naval
     :frigate {:description "Frigate"
               :cost 300
               :movement 8
               :can-capture false
               :can-repair #{}
               :min-range 1
               :max-range 1
               :armor-type :naval
               :armor 8
               :repair 1
               :state-map :free-attack-twice
               :buildable-at #{:seaport}
               :image "tilesets/elite-command/units/frigate-COLOR.png"
               :terrain-effects
               {:deep-water    {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}
                :shallow-water {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}
                :seaport       {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}}
               :attack-strengths
               {:personnel 9
                :armored   9
                :naval     8
                :air       0}
               :zoc
               [:personnel :armored]}
     :destroyer {:description "Destroyer"
                 :cost 600
                 :movement 5
                 :can-capture false
                 :can-repair #{}
                 :min-range 1
                 :max-range 4
                 :armor-type :naval
                 :armor 12
                 :repair 1
                 :state-map :free-attack-once
                 :buildable-at #{:seaport}
                 :image "tilesets/elite-command/units/destroyer-COLOR.png"
                 :terrain-effects
                 {:deep-water {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}
                  :seaport    {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}}
                 :attack-strengths
                 {:personnel 10
                  :armored   12
                  :naval     8
                  :air       0}
                 :zoc
                 [:personnel :armored]}
     :cruiser {:description "Cruiser"
               :cost 1000
               :movement 4
               :can-capture false
               :can-repair #{}
               :min-range 1
               :max-range 3
               :armor-type :naval
               :armor 15
               :repair 1
               :state-map :move-attack-twice
               :buildable-at #{:seaport}
               :image "tilesets/elite-command/units/cruiser-COLOR.png"
               :terrain-effects
               {:deep-water {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}
                :seaport    {:movement-cost 1 :armor-bonus 0 :attack-bonus 0}}
               :attack-strengths
               {:personnel 14
                :armored   16
                :naval     12
                :air       14}
               :zoc
               [:personnel :armored]}
     ;; Air
     :fighter {:description "Fighter"
               :cost 450
               :movement 24
               :can-capture false
               :can-repair #{}
               :min-range 1
               :max-range 1
               :armor-type :air
               :armor 12
               :repair 1
               :state-map :free-attack-twice
               :buildable-at #{:airfield}
               :image "tilesets/elite-command/units/fighter-COLOR.png"
               :terrain-effects
               {:plains        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :mountains     {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :woods         {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :desert        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :tundra        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :swamp         {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :ford          {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :deep-water    {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :shallow-water {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :base          {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :seaport       {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :airfield      {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}}
               :attack-strengths
               {:personnel 4
                :armored   6
                :naval     8
                :air       16}}
     :bomber {:description "Bomber"
              :cost 650
              :movement 24
              :can-capture false
              :can-repair #{}
              :min-range 1
              :max-range 1
              :armor-type :air
              :armor 10
              :repair 1
              :state-map :free-attack-twice
              :buildable-at #{:airfield}
              :image "tilesets/elite-command/units/bomber-COLOR.png"
              :terrain-effects
              {:plains        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :mountains     {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :woods         {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :desert        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :tundra        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :swamp         {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :ford          {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :deep-water    {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :shallow-water {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :base          {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :seaport       {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
               :airfield      {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}}
              :attack-strengths
              {:personnel 14
               :armored   16
               :naval     15
               :air       4}}
     :gunship {:description "Gunship"
               :cost 350
               :movement 15
               :can-capture false
               :can-repair #{}
               :min-range 1
               :max-range 1
               :armor-type :air
               :armor 8
               :repair 1
               :state-map :free-attack-twice
               :buildable-at #{:airfield}
               :image "tilesets/elite-command/units/gunship-COLOR.png"
               :terrain-effects
               {:plains        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :mountains     {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :woods         {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :desert        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :tundra        {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :swamp         {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :ford          {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :deep-water    {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :shallow-water {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :base          {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :seaport       {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}
                :airfield      {:movement-cost 3 :armor-bonus 0 :attack-bonus 0}}
               :attack-strengths
               {:personnel 18
                :armored   10
                :naval     9
                :air       8}
               :zoc
               [:personnel :armored :naval]}}}})

;; TODO: remove redundant id keys (?)
(def maps
  {:sterlings-aruba
   {:id :sterlings-aruba
    :description "Sterling's Aruba"
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
      :terrain-type :seaport #_:mountains
      }
     {:q 2
      :r 4
      :terrain-type :airfield
      ;:terrain-type :plains
      }
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
      :terrain-type :airfield #_:plains}
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
      :terrain-type :seaport #_:mountains}
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
    :description "City Sprawl"
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
      :terrain-type :desert}
     {:q 12
      :r 3
      :terrain-type :woods}
     {:q 13
      :r 3
      :terrain-type :plains}
     {:q 14
      :r 3
      :terrain-type :woods}
     ;; Row 5
     {:q 0
      :r 4
      :terrain-type :woods}
     {:q 1
      :r 4
      :terrain-type :plains}
     ;; Multiplayer base location (red)
     {:q 2
      :r 4
      :terrain-type :plains}
     {:q 3
      :r 4
      :terrain-type :desert}
     ;; Multiplayer base location (unknown)
     {:q 4
      :r 4
      :terrain-type :plains}
     {:q 5
      :r 4
      :terrain-type :desert}
     {:q 6
      :r 4
      :terrain-type :plains}
     {:q 7
      :r 4
      :terrain-type :woods}
     {:q 8
      :r 4
      :terrain-type :mountains}
     {:q 9
      :r 4
      :terrain-type :plains}
     {:q 10
      :r 4
      :terrain-type :mountains}
     {:q 11
      :r 4
      :terrain-type :desert}
     ;; Multiplayer base location (unowned)
     {:q 12
      :r 4
      :terrain-type :plains}
     {:q 13
      :r 4
      :terrain-type :mountains}
     {:q 14
      :r 4
      :terrain-type :plains}
     ;; Row 6
     {:q 0
      :r 5
      :terrain-type :woods}
     {:q 1
      :r 5
      :terrain-type :plains}
     {:q 2
      :r 5
      :terrain-type :plains}
     {:q 3
      :r 5
      :terrain-type :plains}
     {:q 4
      :r 5
      :terrain-type :woods}
     {:q 5
      :r 5
      :terrain-type :plains}
     {:q 6
      :r 5
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 7
      :r 5
      :terrain-type :plains}
     {:q 8
      :r 5
      :terrain-type :plains}
     {:q 9
      :r 5
      :terrain-type :plains}
     {:q 10
      :r 5
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 11
      :r 5
      :terrain-type :plains}
     {:q 12
      :r 5
      :terrain-type :desert}
     ;; Multiplayer base location (blue)
     {:q 13
      :r 5
      :terrain-type :plains}
     {:q 14
      :r 5
      :terrain-type :woods}
     ;; Row 7
     {:q 1
      :r 6
      :terrain-type :woods}
     {:q 2
      :r 6
      :terrain-type :woods}
     {:q 3
      :r 6
      :terrain-type :plains}
     {:q 4
      :r 6
      :terrain-type :woods}
     {:q 5
      :r 6
      :terrain-type :mountains}
     {:q 6
      :r 6
      :terrain-type :plains}
     {:q 7
      :r 6
      :terrain-type :plains}
     {:q 8
      :r 6
      :terrain-type :woods}
     {:q 9
      :r 6
      :terrain-type :plains}
     {:q 10
      :r 6
      :terrain-type :mountains}
     {:q 11
      :r 6
      :terrain-type :plains}
     {:q 12
      :r 6
      :terrain-type :plains}
     {:q 13
      :r 6
      :terrain-type :mountains}
     {:q 14
      :r 6
      :terrain-type :plains}
     ;; Row 8
     {:q 1
      :r 7
      :terrain-type :woods}
     {:q 2
      :r 7
      :terrain-type :woods}
     {:q 3
      :r 7
      :terrain-type :plains}
     {:q 4
      :r 7
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 5
      :r 7
      :terrain-type :plains}
     {:q 6
      :r 7
      :terrain-type :mountains}
     ;; Multiplayer base location (unowned)
     {:q 7
      :r 7
      :terrain-type :plains}
     {:q 8
      :r 7
      :terrain-type :mountains}
     ;; Multiplayer base location (unowned)
     {:q 9
      :r 7
      :terrain-type :plains}
     {:q 10
      :r 7
      :terrain-type :plains}
     {:q 11
      :r 7
      :terrain-type :plains}
     {:q 12
      :r 7
      :terrain-type :mountains}
     {:q 13
      :r 7
      :terrain-type :plains}
     {:q 14
      :r 7
      :terrain-type :woods}
     ;; Row 9
     {:q 1
      :r 8
      :terrain-type :woods}
     {:q 2
      :r 8
      :terrain-type :woods}
     {:q 3
      :r 8
      :terrain-type :plains}
     {:q 4
      :r 8
      :terrain-type :mountains}
     {:q 5
      :r 8
      :terrain-type :plains}
     {:q 6
      :r 8
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 7
      :r 8
      :terrain-type :plains}
     {:q 8
      :r 8
      :terrain-type :plains}
     {:q 9
      :r 8
      :terrain-type :plains}
     {:q 10
      :r 8
      :terrain-type :mountains}
     {:q 11
      :r 8
      :terrain-type :plains}
     {:q 12
      :r 8
      :terrain-type :plains}
     {:q 13
      :r 8
      :terrain-type :plains}
     {:q 14
      :r 8
      :terrain-type :woods}
     ;; Row 10
     {:q 0
      :r 9
      :terrain-type :woods}
     {:q 1
      :r 9
      :terrain-type :plains}
     {:q 2
      :r 9
      :terrain-type :plains}
     {:q 3
      :r 9
      :terrain-type :desert}
     ;; Multiplayer base location (unowned)
     {:q 4
      :r 9
      :terrain-type :woods}
     {:q 5
      :r 9
      :terrain-type :plains}
     {:q 6
      :r 9
      :terrain-type :plains}
     {:q 7
      :r 9
      :terrain-type :mountains}
     {:q 8
      :r 9
      :terrain-type :plains}
     {:q 9
      :r 9
      :terrain-type :desert}
     {:q 10
      :r 9
      :terrain-type :woods}
     {:q 11
      :r 9
      :terrain-type :woods}
     ;; Multiplayer base location (unowned)
     {:q 12
      :r 9
      :terrain-type :plains}
     {:q 13
      :r 9
      :terrain-type :plains}
     {:q 14
      :r 9
      :terrain-type :woods}
     ;; Row 11
     {:q 0
      :r 10
      :terrain-type :woods}
     {:q 1
      :r 10
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 2
      :r 10
      :terrain-type :plains}
     {:q 3
      :r 10
      :terrain-type :desert}
     {:q 4
      :r 10
      :terrain-type :desert}
     {:q 5
      :r 10
      :terrain-type :plains}
     {:q 6
      :r 10
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 7
      :r 10
      :terrain-type :plains}
     {:q 8
      :r 10
      :terrain-type :woods}
     {:q 9
      :r 10
      :terrain-type :plains}
     {:q 10
      :r 10
      :terrain-type :desert}
     ;; Multiplayer base location (unowned)
     {:q 11
      :r 10
      :terrain-type :plains}
     {:q 12
      :r 10
      :terrain-type :desert}
     {:q 13
      :r 10
      :terrain-type :plains}
     {:q 14
      :r 10
      :terrain-type :plains}
     ;; Row 12
     {:q 0
      :r 11
      :terrain-type :plains}
     {:q 1
      :r 11
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 2
      :r 11
      :terrain-type :plains}
     ;; Multiplayer base location (yellow)
     {:q 3
      :r 11
      :terrain-type :plains}
     ;; Multiplayer base location (unowned)
     {:q 4
      :r 11
      :terrain-type :plains}
     {:q 5
      :r 11
      :terrain-type :plains}
     {:q 6
      :r 11
      :terrain-type :mountains}
     {:q 7
      :r 11
      :terrain-type :mountains}
     {:q 8
      :r 11
      :terrain-type :woods}
     ;; Multiplayer base location (unowned)
     {:q 9
      :r 11
      :terrain-type :plains}
     {:q 10
      :r 11
      :terrain-type :mountains}
     ;; Multiplayer base location (pink)
     {:q 11
      :r 11
      :terrain-type :plains}
     {:q 12
      :r 11
      :terrain-type :plains}
     {:q 13
      :r 11
      :terrain-type :plains}
     {:q 14
      :r 11
      :terrain-type :woods}
     ;; Row 13
     {:q 0
      :r 12
      :terrain-type :woods}
     {:q 1
      :r 12
      :terrain-type :woods}
     {:q 2
      :r 12
      :terrain-type :woods}
     {:q 3
      :r 12
      :terrain-type :mountains}
     {:q 4
      :r 12
      :terrain-type :desert}
     {:q 5
      :r 12
      :terrain-type :mountains}
     {:q 6
      :r 12
      :terrain-type :plains}
     {:q 7
      :r 12
      :terrain-type :plains}
     {:q 8
      :r 12
      :terrain-type :plains}
     {:q 9
      :r 12
      :terrain-type :plains}
     {:q 10
      :r 12
      :terrain-type :plains}
     {:q 11
      :r 12
      :terrain-type :desert}
     {:q 12
      :r 12
      :terrain-type :plains}
     {:q 13
      :r 12
      :terrain-type :woods}
     {:q 14
      :r 12
      :terrain-type :woods}
     ;; Row 14
     {:q 1
      :r 13
      :terrain-type :woods}
     {:q 2
      :r 13
      :terrain-type :plains}
     ;; Multiplayer base location (yellow)
     {:q 3
      :r 13
      :terrain-type :plains}
     {:q 4
      :r 13
      :terrain-type :plains}
     {:q 5
      :r 13
      :terrain-type :woods}
     {:q 6
      :r 13
      :terrain-type :woods}
     {:q 7
      :r 13
      :terrain-type :woods}
     {:q 8
      :r 13
      :terrain-type :woods}
     {:q 9
      :r 13
      :terrain-type :plains}
     ;; Multiplayer base location (pink)
     {:q 10
      :r 13
      :terrain-type :plains}
     {:q 11
      :r 13
      :terrain-type :plains}
     {:q 12
      :r 13
      :terrain-type :woods}
     ;; Row 15
     {:q 2
      :r 14
      :terrain-type :woods}
     {:q 3
      :r 14
      :terrain-type :plains}
     {:q 4
      :r 14
      :terrain-type :plains}
     {:q 5
      :r 14
      :terrain-type :woods}
     {:q 6
      :r 14
      :terrain-type :woods}
     {:q 8
      :r 14
      :terrain-type :woods}
     {:q 9
      :r 14
      :terrain-type :woods}
     {:q 10
      :r 14
      :terrain-type :plains}
     {:q 11
      :r 14
      :terrain-type :plains}
     {:q 12
      :r 14
      :terrain-type :woods}
     ]
    }
   })

;; TODO: add support for :allowed-unit-types
(def scenarios
  {:sterlings-aruba-multiplayer
   {:id :sterlings-aruba-multiplayer
    :description "Sterling's Aruba Multiplayer"
    :ruleset-id :zetawar
    :map-id :sterlings-aruba
    :max-count-per-unit 10
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
               :unit-type :infantry}]}]}

   :city-sprawl-multiplayer
   {:id :city-sprawl-multiplayer
    :description "City Sprawl Multiplayer"
    :ruleset-id :zetawar
    :map-id :city-sprawl
    :max-count-per-unit 10
    :credits-per-base 50
    :bases
    [{:q 4  :r 2}
     {:q 12 :r 2}
     {:q 3  :r 3}
     {:q 5  :r 3}
     {:q 10 :r 3}
     {:q 2  :r 4}
     {:q 4  :r 4}
     {:q 12 :r 4}
     {:q 7  :r 5}
     {:q 11 :r 5}
     {:q 13 :r 5}
     {:q 5  :r 7}
     {:q 7  :r 7}
     {:q 9  :r 7}
     {:q 7  :r 8}
     {:q 12 :r 9}
     {:q 2  :r 10}
     {:q 7  :r 10}
     {:q 11 :r 10}
     {:q 2  :r 11}
     {:q 3  :r 11}
     {:q 4  :r 11}
     {:q 9  :r 11}
     {:q 11 :r 11}
     {:q 3  :r 13}
     {:q 10 :r 13}]
    ;; TODO: determine best faction order
    :factions
    [{:color :red
      :credits 300
      :ai false
      :bases [{:q 3  :r 3}
              {:q 2  :r 4}]
      :units [{:q 3 :r 2
               :unit-type :infantry}
              {:q 2 :r 3
               :unit-type :infantry}]}
     {:color :blue
      :credits 300
      :ai true
      :bases [{:q 12 :r 4}
              {:q 13 :r 5}]
      :units [{:q 12 :r 3
               :unit-type :infantry}
              {:q 13 :r 4
               :unit-type :infantry}]}
     {:color :yellow
      :credits 300
      :ai true
      :bases [{:q 3 :r 11}
              {:q 3 :r 13}]
      :units [{:q 2 :r 12
               :unit-type :infantry}
              {:q 3 :r 12
               :unit-type :infantry}]}
     {:color :pink
      :credits 300
      :ai true
      :bases [{:q 11 :r 11}
              {:q 10 :r 13}]
      :units [{:q 10 :r 11
               :unit-type :infantry}
              {:q 11 :r 12
               :unit-type :infantry}]}]}})
