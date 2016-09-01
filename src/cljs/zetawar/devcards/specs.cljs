(ns zetawar.devcards.specs
  (:require
    [clojure.spec :as s]
    [clojure.test.check.generators]
    [devcards.core :as dc :include-macros true]
    [zetawar.map-spec]
    [zetawar.events-spec])
  (:require-macros
    [devcards.core :refer [defcard defcard-rg]]))

(defcard map-spec
  (s/exercise :zetawar/map 5))

(defcard handler-spec
  (s/exercise :zetawar.events/select-hex 5))
