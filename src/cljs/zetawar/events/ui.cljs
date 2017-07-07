(ns zetawar.events.ui
  (:require
   [cljsjs.clipboard]
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.app :as app]
   [zetawar.db :refer [e qe qes qess]]
   [zetawar.game :as game]
   [zetawar.players :as players]
   [zetawar.router :as router]
   [zetawar.util :refer [breakpoint inspect only oonly]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Global

(defmethod router/handle-event ::alert
  [{:as handler-ctx :keys [db]} [_ message]]
  (let [app (app/root db)]
    {:tx [{:db/id (e app)
           :app/alert-type :success
           :app/alert-message message}]}))

(defmethod router/handle-event ::hide-alert
  [{:as handler-ctx :keys [db]} [_]]
  (let [app (app/root db)]
    {:tx [[:db/retract (e app) :app/alert-message (:app/alert-message app)]
          [:db/retract (e app) :app/alert-type (:app/alert-type app)]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Selection

;; New click logic:
;; - no selection?
;;   - owned unit on tile?
;;     - select unit
;;   - owned base?
;;     - select base
;; - already this tile selected?
;;   - deselect
;; - selection set, but this tile not selected?
;;   - selected tile a unit?
;;     - tile contains enemy in range?
;;       - set target to enemy
;;     - tile is a valid move destination?
;;       - set target to terrain

(defmethod router/handle-event ::select-hex
  [{:as handler-ctx :keys [db]} [_ ev-q ev-r]]
  (let [app (app/root db)
        game (app/current-game db)
        [selected-q selected-r] (app/selected-hex db)
        [targeted-q targeted-r] (app/targeted-hex db)
        unit (game/unit-at db game ev-q ev-r)
        terrain (game/terrain-at db game ev-q ev-r)
        selected-unit (game/unit-at db game selected-q selected-r)
        selected-terrain (game/terrain-at db game selected-q selected-r)
        targeted-unit (game/unit-at db game targeted-q targeted-r)
        targeted-terrain (game/terrain-at db game targeted-q targeted-r)]
    {:tx (cond
           ;; selecting selected tile
           (and (= ev-q selected-q) (= ev-r selected-r))
           (cond-> []
             (and selected-q selected-r)
             (conj [:db/retract (e app) :app/selected-q selected-q]
                   [:db/retract (e app) :app/selected-r selected-r])

             (and targeted-q targeted-r)
             (conj [:db/retract (e app) :app/targeted-q targeted-q]
                   [:db/retract (e app) :app/targeted-r targeted-r]))

           ;; selecting targeted tile
           (and (= ev-q targeted-q) (= ev-r targeted-r))
           [[:db/retract (e app) :app/targeted-q targeted-q]
            [:db/retract (e app) :app/targeted-r targeted-r]]

           ;; selecting in range enemy unit with unit selected
           (and unit
                selected-unit
                (not (game/unit-current? db game unit))
                (and (game/can-attack? db game selected-unit)
                     (game/in-range? db selected-unit unit)))
           [{:db/id (e app)
             :app/targeted-q ev-q
             :app/targeted-r ev-r}]

           ;; selecting valid destination terrain with unit selected
           (and terrain
                selected-unit
                (game/can-move? db game selected-unit)
                (game/valid-destination? db game selected-unit ev-q ev-r))
           [{:db/id (e app)
             :app/targeted-q ev-q
             :app/targeted-r ev-r}]

           ;; selecting friendly unit with unit or terrain selected
           (and unit
                (or selected-unit selected-terrain)
                (game/can-field-repair? db game selected-unit)
                (game/repairable? db game unit)
                (game/in-range? db selected-unit unit)
                (game/has-repairable-armor-type? db game selected-unit unit))
             [{:db/id (e app)
               :app/targeted-q ev-q
               :app/targeted-r ev-r}]

           ;; selecting owned base with no unit selected
           (and terrain
                (not unit)
                (not selected-unit)
                (game/current-base? db game terrain))
           (cond-> [{:db/id (e app)
                     :app/selected-q ev-q
                     :app/selected-r ev-r}]
             (and targeted-q targeted-r)
             (conj [:db/retract (e app) :app/targeted-q targeted-q]
                   [:db/retract (e app) :app/targeted-r targeted-r]))

           ;; selecting unselected friendly unit
           (and unit
                (or (game/can-move? db game unit)
                    (and
                     (game/can-attack? db game unit)
                     (not= 0 (count (game/enemies-in-range db game unit))))
                    (and
                      (game/can-field-repair? db game unit)
                      (not= 0 (count (game/friends-in-range db game unit))))
                    (game/can-capture? db game unit terrain)))
           (cond-> [{:db/id (e app)
                     :app/selected-q ev-q
                     :app/selected-r ev-r}]
             (and targeted-q targeted-r)
             (conj [:db/retract (e app) :app/targeted-q targeted-q]
                   [:db/retract (e app) :app/targeted-r targeted-r])))}))

(defmethod router/handle-event ::clear-selection
  [{:as handler-ctx :keys [db]} _]
  (let [app (app/root db)
        [selected-q selected-r] (app/selected-hex db)
        [targeted-q targeted-r] (app/targeted-hex db)]
    {:tx (cond-> []
           (and selected-q selected-r)
           (conj [:db/retract (e app) :app/selected-q selected-q]
                 [:db/retract (e app) :app/selected-r selected-r])

           (and targeted-q targeted-r)
           (conj [:db/retract (e app) :app/targeted-q targeted-q]
                 [:db/retract (e app) :app/targeted-r targeted-r]))}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Unit and base actions

(defmethod router/handle-event ::move-selected-unit
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [from-q from-r] (app/selected-hex db)
        [to-q to-r] (app/targeted-hex db)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/move-unit
                  :action/faction-color cur-faction-color
                  :action/from-q from-q
                  :action/from-r from-r
                  :action/to-q to-q
                  :action/to-r to-r}]
                [::move-selection from-q from-r to-q to-r]]}))

;; TODO: replace with move selection to target
(defmethod router/handle-event ::move-selection
  [{:as handler-ctx :keys [db]} [_ from-q from-r to-q to-r]]
  (let [app (app/root db)
        game (app/current-game db)
        unit (game/unit-at db game to-q to-r)
        terrain (game/base-at db game to-q to-r)]
    (if (or (and (game/can-attack? db game unit)
                 (not-empty (game/enemies-in-range db game unit)))
            (and (game/can-field-repair? db game unit)
                 (not-empty (game/repairable-friends-in-range db game unit)))
            (game/can-capture? db game unit terrain))
      {:tx       [[:db/add (e app) :app/selected-q to-q]
                  [:db/add (e app) :app/selected-r to-r]
                  [:db/retract (e app) :app/targeted-q to-q]
                  [:db/retract (e app) :app/targeted-r to-r]]}
      {:dispatch [[::clear-selection]]})))

(defmethod router/handle-event ::attack-targeted
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [attacker-q attacker-r] (app/selected-hex db)
        [defender-q defender-r] (app/targeted-hex db)
        [attacker-damage defender-damage] (game/battle-damage db game
                                                              attacker-q attacker-r
                                                              defender-q defender-r)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/attack-unit
                  :action/faction-color cur-faction-color
                  :action/attacker-q attacker-q
                  :action/attacker-r attacker-r
                  :action/defender-q defender-q
                  :action/defender-r defender-r
                  :action/attacker-damage attacker-damage
                  :action/defender-damage defender-damage}]
                [::clear-selection]]}))

(defmethod router/handle-event ::repair-selected
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [q r] (app/selected-hex db)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/repair-unit
                  :action/faction-color cur-faction-color
                  :action/q q
                  :action/r r}]
                [::clear-selection]]}))

(defmethod router/handle-event ::repair-targeted
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [repairer-q repairer-r] (app/selected-hex db)
        [target-q target-r] (app/targeted-hex db)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/field-repair-unit
                  :action/faction-color cur-faction-color
                  :action/repairer-q repairer-q
                  :action/repairer-r repairer-r
                  :action/target-q target-q
                  :action/target-r target-r}]
                [::clear-selection]]}))

(defmethod router/handle-event ::capture-selected
  [{:as handler-ctx :keys [db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [q r] (app/selected-hex db)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/capture-base
                  :action/faction-color cur-faction-color
                  :action/q q
                  :action/r r}]
                [::clear-selection]]}))

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} [_ unit-type-id]]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)
        [q r] (app/selected-hex db)]
    {:dispatch [[:zetawar.events.game/execute-action
                 {:action/type :action.type/build-unit
                  :action/faction-color cur-faction-color
                  :action/q q
                  :action/r r
                  :action/unit-type-id unit-type-id}]
                [::clear-selection]]}))

(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [ev-chan notify-chan conn db]} _]
  (let [game (app/current-game db)
        cur-faction-color (game/current-faction-color game)]
    {:dispatch [[::clear-selection]
                [:zetawar.events.game/execute-action
                 {:action/type :action.type/end-turn
                  :action/faction-color cur-faction-color}]]}))

(defmethod router/handle-event ::set-url-game-state
  [{:as handler-ctx :keys [ev-chan conn db]} _]
  (app/set-url-game-state! @conn))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Copy link

(defmethod router/handle-event ::show-copy-link
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/show-copy-link true]]}))

(defmethod router/handle-event ::hide-copy-link
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)
        {:keys [app/show-copy-link]} app]
    (when-not (nil? show-copy-link)
      {:tx [[:db/retract (e app) :app/show-copy-link show-copy-link]]})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Unit picker

(defmethod router/handle-event ::show-unit-picker
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/picking-unit true]]}))

(defmethod router/handle-event ::hide-unit-picker
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/picking-unit false]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Win message

(defmethod router/handle-event ::hide-win-message
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/hide-win-message true]]}))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Faction configuration

(defmethod router/handle-event ::configure-faction
  [{:as handler-ctx :keys [ev-chan db]} [_ faction]]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/configuring-faction (e faction)]]}))

(defmethod router/handle-event ::hide-faction-settings
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)
        faction-eid (-> app :app/configuring-faction e)]
    {:tx [[:db/retract (e app) :app/configuring-faction faction-eid]]}))

;; TODO: find a way to make player swapping nicer (maybe put in router?)
;; TODO: cleanup return value construction
(defmethod router/handle-event ::set-faction-player-type
  [{:as handler-ctx :keys [ev-chan conn db players]} [_ faction player-type-id]]
  (let [{:as app :keys [ai-turn-stepping]} (app/root db)
        {:keys [game/factions game/current-faction]} (app/current-game db)
        {:keys [faction/color]} faction
        other-factions (remove #(= (e faction) (e %)) factions)
        {:keys [ai]} (players/player-types-by-id player-type-id)
        tx [{:db/id (e faction)
             :faction/ai ai
             :faction/player-type player-type-id}]
        cur-player (color @players)
        new-player (players/new-player handler-ctx player-type-id color)
        notify (when (and ai (= (e faction) (e current-faction)))
                 [[:zetawar.players/start-turn color]])]
    (players/stop cur-player)
    (players/start new-player)
    (swap! players assoc color new-player)
    (if (and ai (= (count other-factions)
                   (count (filter :faction/ai other-factions))))
      {:tx (conj tx [:db/add (e app) :app/ai-turn-stepping (not ai-turn-stepping)])
       :dispatch [[::alert "AI enabled for all factions. Enabling turn stepping."]]
       :notify notify}
      {:tx (conj tx [:db/add (e app) :app/ai-turn-stepping false])
       :notify notify})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; New game

(defmethod router/handle-event ::show-new-game-settings
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/configuring-new-game true]]}))

(defmethod router/handle-event ::hide-new-game-settings
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/configuring-new-game false]]}))

(defmethod router/handle-event ::start-new-game
  [{:as handler-ctx :keys [ev-chan conn]} [_ scenario-id]]
  (app/start-new-game! handler-ctx scenario-id))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; End turn alert

(defmethod router/handle-event ::show-end-turn-alert
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/end-turn-alert true]]}))

(defmethod router/handle-event ::hide-end-turn-alert
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/end-turn-alert false]]}))
