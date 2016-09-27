(ns zetawar.dev
  (:require
    [devcards.core :as dc :include-macros true]
    [devtools.core :as devtools]
    [zetawar.devcards.maps]
    [zetawar.devcards.prototype]
    [zetawar.devcards.selection-and-target]
    [zetawar.devcards.specs]
    [zetawar.game-test]
    [zetawar.site :as site]
    [zetawar.subs-test]))

(enable-console-print!)

(devtools/install!)

(when (site/viewing-devcards?)
  (dc/start-devcard-ui!))
