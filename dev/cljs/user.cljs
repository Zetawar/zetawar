(ns cljs.user
  (:require
   [datascript.core :as d]
   [zetawar.core :as core]
   [zetawar.game :as game]
   [zetawar.util :as util]))

(comment

  ;; capturing units
  (doall
   (d/q '[:find ?u
          :in $ ?r
          :where
          [_ :game/current-faction ?f]
          [?f :faction/units ?u]
          [?u :unit/capturing true]
          [?u :unit/capture-round ?r]]
        @core/conn 1))

  )
