(ns zetawar.dev
  (:require
    [devcards.core :as dc :include-macros true]
    [zetawar.devcards.maps]
    [zetawar.devcards.prototype]
    [zetawar.devcards.selection-and-target]
    [zetawar.game-test]
    [zetawar.subs-test]
    [zetawar.site :as site]))

(enable-console-print!)

(when (site/viewing-devcards?)
  (dc/start-devcard-ui!))
