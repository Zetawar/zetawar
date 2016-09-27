(ns zetawar.map-spec
  (:require
    [clojure.spec :as s]
    [zetawar.hex-spec :as hex-spec]
    [zetawar.hex :as hex]
    [zetawar.map :as map]))

(s/def ::map/id keyword?)
(s/def ::map/name string?)

(s/def :zetawar/terrain-type #{:zetawar.terrain-type/deep-water
                               :zetawar.terrain-type/mountains
                               :zetawar.terrain-type/plains
                               :zetawar.terrain-type/woods})

(s/def :zetawar/terrain (s/keys :req [::hex/q
                                      ::hex/r
                                      :zetawar/terrain-type]))

(s/def ::map/terrains (s/+ :zetawar/terrain))

(s/def :zetawar/map (s/keys :req [::map/id
                                  ::map/name
                                  ::map/terrains]))

;; TODO: require connected graph
