(ns zetawar.hex-spec
  (:require
    [clojure.spec :as s]
    [zetawar.hex :as hex]))

(s/def ::hex/q (s/int-in hex/min-q hex/max-q))
(s/def ::hex/r (s/int-in hex/min-r hex/max-r))
