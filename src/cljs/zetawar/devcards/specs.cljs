(ns zetawar.devcards.specs
  (:require
   [clojure.spec :as s]
   [clojure.test.check.generators]
   [devcards.core :as dc :include-macros true]
   [zetawar.system.spec :as system.spec]
   [zetawar.game.spec :as game.spec])
  (:require-macros
   [devcards.core :refer [defcard defcard-rg]]))

;; (defcard system-spec
;;   (s/exercise :zetawar/system 5))

(defcard game-map-spec
  (first
   (s/exercise :game/map 1)))

(comment

  (defcard map-spec
    (s/exercise :zetawar/map 5))

  (defcard handler-spec
    (s/exercise :zetawar.ui.events/select-hex 5))

  )
