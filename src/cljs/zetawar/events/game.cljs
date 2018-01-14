(ns zetawar.events.game
  (:require
   [datascript.core :as d]
   [zetawar.app :as app]
   [zetawar.game :as game]
   [zetawar.logging :as log]
   [zetawar.router :as router]))

;; TODO: simplify
(defmethod router/handle-event ::execute-action
  [{:as handler-ctx :keys [db]} [_ action]]
  (let [game (app/current-game db)]
    (case (:action/type action)
      :action.type/attack-unit
      (let [{:keys [action/attacker-q action/attacker-r
                    action/defender-q action/defender-r]} action
            [attacker-damage defender-damage] (game/battle-damage db game
                                                                  attacker-q attacker-r
                                                                  defender-q defender-r)
            action (merge action {:action/attacker-damage attacker-damage
                                  :action/defender-damage defender-damage})]
        {:tx       (game/action-tx db game action)
         :dispatch [[:zetawar.events.ui/hide-copy-link]]
         :notify   [[:zetawar.players/apply-action :faction.color/all action]]})

      :action.type/end-turn
      (let [game (app/current-game db)
            next-faction-color (game/next-faction-color game)]
        {:tx       (game/action-tx db game action)
         :dispatch (cond-> [[:zetawar.events.ui/set-url-game-state]]
                     (<= 2 (game/human-faction-count db game))
                     (into [[:zetawar.events.ui/show-copy-link]]))
         :notify   [[:zetawar.players/apply-action :faction.color/all action]
                    [:zetawar.players/start-turn next-faction-color]]})

      {:tx       (game/action-tx db game action)
       :dispatch [[:zetawar.events.ui/hide-copy-link]]
       :notify   [[:zetawar.players/apply-action :faction.color/all action]]})))
