(ns zetawar.devcards
  (:require
    [devcards.core :as dc :include-macros true]
    [zetawar.devcards.maps]
    [zetawar.devcards.prototype]
    [zetawar.devcards.selection-and-target]
    [zetawar.game-test]
    [zetawar.subs-test]))

(dc/start-devcard-ui!)
