(ns zetawar.data)

;; TODO: add unit/terrain

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
                   :image "tilesets/elite-command/terrains/shallow-water.png"}})

(def units
  {:infantry {:name "Infantry"
              :credits 75
              :movement 9
              :can-capture true
              :min-range 1
              :max-range 1
              :armor-type :personnel
              :armor 6
              :image "elite-command/units/infantry-COLOR.png"
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

(def maps
  {:sterlings-aruba
   [;; Map

    {:db/id -101
     :map/id :sterlings-aruba
     :map/name "Sterling's Aruba"
     :map/initial-credits 300
     :map/credits-per-base 100
     :game/_map :game}

    ;; Terrains

    ;; Row 1
    {:db/id -201
     :terrain/q 1
     :terrain/r 0
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -202
     :terrain/q 2
     :terrain/r 0
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -203
     :terrain/q 3
     :terrain/r 0
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -204
     :terrain/q 4
     :terrain/r 0
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -205
     :terrain/q 5
     :terrain/r 0
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -206
     :terrain/q 6
     :terrain/r 0
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    ;; Row 2
    {:db/id -207
     :terrain/q 0
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -208
     :terrain/q 1
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -209
     :terrain/q 2
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/base]
     :map/_terrains -101}
    {:db/id -210
     :terrain/q 3
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -211
     :terrain/q 4
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -212
     :terrain/q 5
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/woods]
     :map/_terrains -101}
    {:db/id -213
     :terrain/q 6
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -214
     :terrain/q 7
     :terrain/r 1
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    ;; Row 3
    {:db/id -215
     :terrain/q 0
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -216
     :terrain/q 1
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/base]
     :map/_terrains -101}
    {:db/id -218
     :terrain/q 2
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -219
     :terrain/q 3
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -220
     :terrain/q 4
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -221
     :terrain/q 5
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/woods]
     :map/_terrains -101}
    {:db/id -222
     :terrain/q 6
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -223
     :terrain/q 7
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -224
     :terrain/q 8
     :terrain/r 2
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    ;; Row 4
    {:db/id -225
     :terrain/q 0
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -226
     :terrain/q 1
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -228
     :terrain/q 2
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -229
     :terrain/q 3
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -230
     :terrain/q 4
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/woods]
     :map/_terrains -101}
    {:db/id -231
     :terrain/q 5
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -232
     :terrain/q 6
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -233
     :terrain/q 7
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -234
     :terrain/q 8
     :terrain/r 3
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    ;; Row 5
    {:db/id -235
     :terrain/q 0
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -236
     :terrain/q 1
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -238
     :terrain/q 2
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -239
     :terrain/q 3
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -240
     :terrain/q 4
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -241
     :terrain/q 5
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -242
     :terrain/q 6
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -243
     :terrain/q 7
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -244
     :terrain/q 8
     :terrain/r 4
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    ;; Row 6
    {:db/id -245
     :terrain/q 0
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -246
     :terrain/q 1
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -248
     :terrain/q 2
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -249
     :terrain/q 3
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/woods]
     :map/_terrains -101}
    {:db/id -250
     :terrain/q 4
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -251
     :terrain/q 5
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -252
     :terrain/q 6
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -253
     :terrain/q 7
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -254
     :terrain/q 8
     :terrain/r 5
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    ;; Row 7
    {:db/id -255
     :terrain/q 0
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -256
     :terrain/q 1
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -258
     :terrain/q 2
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -259
     :terrain/q 3
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/woods]
     :map/_terrains -101}
    {:db/id -260
     :terrain/q 4
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -261
     :terrain/q 5
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -262
     :terrain/q 6
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -263
     :terrain/q 7
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/base]
     :map/_terrains -101}
    {:db/id -264
     :terrain/q 8
     :terrain/r 6
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    ;; Row 8
    {:db/id -265
     :terrain/q 0
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -266
     :terrain/q 1
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -268
     :terrain/q 2
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/woods]
     :map/_terrains -101}
    {:db/id -269
     :terrain/q 3
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -270
     :terrain/q 4
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -271
     :terrain/q 5
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/base]
     :map/_terrains -101}
    {:db/id -272
     :terrain/q 6
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -273
     :terrain/q 7
     :terrain/r 7
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    ;; Row 9
    {:db/id -274
     :terrain/q 1
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -275
     :terrain/q 2
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -276
     :terrain/q 3
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -277
     :terrain/q 4
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/deep-water]
     :map/_terrains -101}
    {:db/id -278
     :terrain/q 5
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/mountains]
     :map/_terrains -101}
    {:db/id -279
     :terrain/q 6
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}
    {:db/id -280
     :terrain/q 7
     :terrain/r 8
     :terrain/type [:terrain-type/id :terrain-type.id/plains]
     :map/_terrains -101}

    ;; Factions

    {:db/id -301
     :faction/color :faction.color/blue
     :faction/order 1
     :faction/credits 300
     :faction/ai false
     :map/_starting-faction -101
     :game/_factions :game
     :game/_current-faction :game
     :terrain/_owner -216}

    {:db/id -302
     :faction/color :faction.color/red
     :faction/order 2
     :faction/credits 300
     :faction/ai true
     :game/_factions :game
     :terrain/_owner -263
     :faction/_next-faction -301
     :faction/next-faction -301}

    ;; Units

    {:db/id -401
     :unit/q 2
     :unit/r 2
     :unit/type [:unit-type/id :unit-type.id/infantry]
     :unit/count 10
     :unit/round-built 0
     :unit/move-count 0
     :unit/attack-count 0
     :unit/repaired false
     :unit/capturing false
     :faction/_units -301}

    {:db/id -402
     :unit/q 7
     :unit/r 7
     :unit/type [:unit-type/id :unit-type.id/infantry]
     :unit/count 10
     :unit/round-built 0
     :unit/move-count 0
     :unit/attack-count 0
     :unit/repaired false
     :unit/capturing false
     :faction/_units -302}

    {:db/id -403
     :unit/q 7
     :unit/r 8
     :unit/type [:unit-type/id :unit-type.id/infantry]
     :unit/count 10
     :unit/round-built 0
     :unit/move-count 0
     :unit/attack-count 0
     :unit/capturing false
     :unit/repaired false
     :faction/_units -302}]
    })

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
