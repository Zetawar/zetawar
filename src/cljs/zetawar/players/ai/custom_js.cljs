(ns zetawar.players.ai.custom-js
  (:require
   [cljs.core.async :as async]
   [datascript.core :as d]
   [zetawar-js-ai]
   [zetawar.app :as app]
   [zetawar.db :as db]
   [zetawar.game :as game]
   [zetawar.hex :as hex]
   [zetawar.logging :as log]
   [zetawar.players :as players]
   [zetawar.players.simple-embedded :refer [simple-embedded-player]]))

(defn mk-actor-ctx [db game actor]
  (js/ZetawarAI.makeActorContext db game actor))

(defn score-actor [db game actor actor-ctx]
  (js/ZetawarAI.scoreActor db game actor actor-ctx))

(defn mk-base-action-ctx [db game actor-ctx base]
  (js/ZetawarAI.makeBaseActionContext db game actor-ctx base))

(defn score-base-action [db game base action-ctx action]
  (js/ZetawarAI.scoreBaseAction db game base action-ctx (clj->js action)))

(defn mk-unit-action-ctx [db game actor-ctx unit]
  (js/ZetawarAI.makeUnitActionContext db game actor-ctx unit))

(defn score-unit-action [db game unit action-ctx action]
  (js/ZetawarAI.scoreUnitAction db game unit action-ctx (clj->js action)))

(defmethod players/new-player ::players/custom-js-ai
  [{:as app-ctx :keys [ev-chan notify-pub]} player-type faction-color]
  (let [fns {:mk-actor-ctx #'mk-actor-ctx
             :score-actor #'score-actor
             :mk-base-action-ctx #'mk-base-action-ctx
             :score-base-action #'score-base-action
             :mk-unit-action-ctx #'mk-unit-action-ctx
             :score-unit-action #'score-unit-action}]
    (simple-embedded-player faction-color
                            ev-chan
                            notify-pub
                            fns)))
