(ns ^:figwheel-hooks zetawar.test-runner
  (:require
   [cljs-test-display.core]
   [cljs.test]
   [zetawar.game-test]
   [zetawar.subs-test]))

(defn ^:after-load test-run []
  (cljs.test/run-tests
   (cljs-test-display.core/init! "app")
   'zetawar.game-test
   'zetawar.subs-test))

(defonce runit (test-run))
