(ns zetawar.devcards
  (:require
   [devcards.core :as devcards :include-macros true]
   [zetawar.devcards.data-formats]
   [zetawar.devcards.maps-and-scenarios]
   [zetawar.devcards.prototype]
   [zetawar.devcards.selection-and-target]
   [zetawar.devcards.specs]
   [zetawar.site :as site]))

(when (site/viewing-devcards?)
  (devcards/start-devcard-ui!))
