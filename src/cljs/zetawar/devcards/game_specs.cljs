(ns zetawar.devcards.game-specs
  (:require
   [clojure.spec :as s]
   [clojure.test.check.generators]
   [devcards.core :as dc :include-macros true]
   [zetawar.game.spec :as game.spec])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

(defcard game-map-spec
  (first
   (s/exercise :game/map 1)))
