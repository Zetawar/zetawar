(ns zetawar.dev
  (:require
   [clojure.spec.test :as spec.test]
   [devtools.core :as devtools]
   [nightlight.repl-server]
   [zetawar.game-test]
   [zetawar.subs-test]
   [zetawar.devcards]))

(enable-console-print!)

(devtools/install!)

(spec.test/instrument)
