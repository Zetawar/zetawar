(ns zetawar.events-spec
  (:require
    [clojure.spec :as s]
    [zetawar.events :as events]
    [zetawar.hex :as hex]))

(s/def :zetawar.event/id #{:zetawar.event.id/select-hex
                           :zetawar.event.id/clear-selection})

(s/def :zetawar.events/select-hex
  (s/and (s/keys :req [:zetawar.event/id
                       ::hex/q
                       ::hex/r])
         #(= (:zetawar.event/id %) :zetawar.event.id/select-hex)))
