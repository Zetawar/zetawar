(ns zetawar.dev
  (:require
   [clojure.spec.test :as spec.test]
   [devcards.core :as devcards :include-macros true]
   [devtools.core :as devtools]
   [nightlight.repl-server]
   [zetawar.devcards.prototype]
   [zetawar.devcards.scenarios]
   [zetawar.devcards.selection-and-target]
   [zetawar.devcards.specs]
   [zetawar.game-test]
   [zetawar.site :as site]
   [zetawar.subs-test]))

(enable-console-print!)

(devtools/install!)

(spec.test/instrument)

(when (site/viewing-devcards?)
  (devcards/start-devcard-ui!))
