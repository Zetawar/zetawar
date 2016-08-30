(ns zetawar.devcards.specs
  (:require
    [devcards.core :as dc :include-macros true]
    [zetawar.map-spec]
    [clojure.spec :as s]
    [clojure.test.check.generators])
  (:require-macros
    [devcards.core :refer [defcard defcard-rg]]))

(defcard map-spec
  (s/exercise :zetawar/map))
