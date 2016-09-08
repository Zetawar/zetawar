(ns zetawar.events-spec
  (:require
    [clojure.spec :as s]
    [zetawar.events :as events]
    [zetawar.hex :as hex]))

(s/def ::events/event-id #{::events/select-hex
                           ::events/clear-selection})

(s/def ::events/select-hex
  (s/and (s/keys :req [::events/event-id
                       ::hex/q
                       ::hex/r])
         #(= (::events/event-id %) ::events/select-hex)))
