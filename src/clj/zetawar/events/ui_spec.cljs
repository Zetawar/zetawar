(ns zetawar.events.ui-spec
  (:require
   [clojure.spec :as s]
   [zetawar.events.ui :as events.ui]
   [zetawar.hex :as hex]))

(s/def :zetawar.events/event-id #{::events.ui/select-hex
                                  ::events.ui/clear-selection})

(s/def ::events.ui/select-hex
  (s/and (s/keys :req [:zetawar.events/event-id
                       ::hex/q
                       ::hex/r])
         #(= (::events.ui/event-id %) ::events.ui/select-hex)))
