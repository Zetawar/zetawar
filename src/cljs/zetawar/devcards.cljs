(ns zetawar.devcards
  (:require
   [devcards.core :as devcards :include-macros true]
   [zetawar.devcards.data-formats]
   [zetawar.devcards.game-specs]
   [zetawar.devcards.maps-and-scenarios]
   [zetawar.devcards.selection-and-target]
   [zetawar.site :as site]))

(when (site/viewing-devcards?)
  (devcards/start-devcard-ui!))
