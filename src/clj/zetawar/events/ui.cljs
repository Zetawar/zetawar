(ns zetawar.events.ui
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.ai :as ai]
   [zetawar.app :as app]
   [zetawar.db :refer [e qe qes qess]]
   [zetawar.events.game :as events.game]
   [zetawar.game :as game]
   [zetawar.router :as router]
   [zetawar.players :as players]
   [zetawar.util :refer [breakpoint inspect only oonly]]))

(defmethod router/handle-event ::alert
  [{:as handler-ctx :keys [ev-chan conn db]} [_ text]]
  (js/alert text))

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
    (inspect [selected-q selected-r])
    {:tx (cond
           ;; selecting selected tile
           (and (= ev-q selected-q) (= ev-r selected-r))
           (cond-> []
             (and selected-q selected-r)
             (->
              (conj [:db/retract (e app) :app/selected-q selected-q]
                    [:db/retract (e app) :app/selected-r selected-r]))

             (and targeted-q targeted-r)
             (->
              (conj [:db/retract (e app) :app/targeted-q targeted-q]
                    [:db/retract (e app) :app/targeted-r targeted-r])))

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
                (or (game/can-move? db game unit)
                    (game/can-attack? db game unit)))
           (cond-> [{:db/id (e app)
                     :app/selected-q ev-q
                     :app/selected-r ev-r}]
             (and targeted-q targeted-r)
             (->
              (conj [:db/retract (e app) :app/targeted-q targeted-q]
                    [:db/retract (e app) :app/targeted-r targeted-r])))

           ;; selecting owned base with no unit selected
           (and terrain
                (not unit)
                (not selected-unit)
                (game/current-base? db game terrain))
           (cond-> [{:db/id (e app)
                     :app/selected-q ev-q
                     :app/selected-r ev-r}]
             (and targeted-q targeted-r)
             (->
              (conj [:db/retract (e app) :app/targeted-q targeted-q]
                    [:db/retract (e app) :app/targeted-r targeted-r])))

           ;; selecting unselected friendly unit
           (and unit
                (or (game/can-move? db game unit)
                    (and
                     (game/can-attack? db game unit)
                     (not= 0 (count (game/enemies-in-range db game unit))))
                    (game/can-capture? db game unit terrain)))
           (cond-> [{:db/id (e app)
                     :app/selected-q ev-q
                     :app/selected-r ev-r}]
             (and targeted-q targeted-r)
             (->
              (conj [:db/retract (e app) :app/targeted-q targeted-q]
                    [:db/retract (e app) :app/targeted-r targeted-r]))))}))

(defmethod router/handle-event ::clear-selection
  [{:as handler-ctx :keys [db]} _]
  (let [app (app/root db)
        [selected-q selected-r] (app/selected-hex db)
        [targeted-q targeted-r] (app/targeted-hex db)]
    {:tx (cond-> []
           (and selected-q selected-r)
           (->
            (conj [:db/retract (e app) :app/selected-q selected-q]
                  [:db/retract (e app) :app/selected-r selected-r]))

           (and targeted-q targeted-r)
           (->
            (conj [:db/retract (e app) :app/targeted-q targeted-q]
                  [:db/retract (e app) :app/targeted-r targeted-r])))}))

(defmethod router/handle-event ::move-selected-unit
  [{:as handler-ctx :keys [db]} _]
  (let [[from-q from-r] (app/selected-hex db)
        [to-q to-r] (app/targeted-hex db)]
    {:dispatch [[::events.game/move-unit from-q from-r to-q to-r]
                [::move-selection from-q from-r to-q to-r]]}))

(defmethod router/handle-event ::move-selection
  [{:as handler-ctx :keys [db]} [_ from-q from-r to-q to-r]]
  (let [app (app/root db)
        game (app/current-game db)
        unit (game/unit-at db game to-q to-r)
        terrain (game/base-at db game to-q to-r)]
    (if (or (and (game/can-attack? db game unit)
                 (not-empty (game/enemies-in-range db game unit)))
            (game/can-capture? db game unit terrain))
      {:tx       [[:db/add (e app) :app/selected-q to-q]
                  [:db/add (e app) :app/selected-r to-r]
                  [:db/retract (e app) :app/targeted-q to-q]
                  [:db/retract (e app) :app/targeted-r to-r]]}
      {:dispatch [[::clear-selection]]})))


(defmethod router/handle-event ::attack-targeted
  [{:as handler-ctx :keys [db]} _]
  (let [[attacker-q attacker-r] (app/selected-hex db)
        [target-q target-r] (app/targeted-hex db)]
    {:dispatch [[::events.game/attack-unit attacker-q attacker-r target-q target-r]
                [::clear-selection]]}))

(defmethod router/handle-event ::repair-selected
  [{:as handler-ctx :keys [db]} _]
  (let [[q r] (app/selected-hex db)]
    {:dispatch [[::events.game/repair-unit q r]
                [::clear-selection]]}))

(defmethod router/handle-event ::capture-selected
  [{:as handler-ctx :keys [db]} _]
  (let [[q r] (app/selected-hex db)]
    {:dispatch [[::events.game/capture-base q r]
                [::clear-selection]]}))

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} _]
  (let [[q r] (app/selected-hex db)]
    {:dispatch [[::events.game/build-unit q r :unit-type.id/infantry]
                [::clear-selection]]}))

;; TODO: convert to pure function
(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [ev-chan notify-chan conn db]} _]
  (let [game-id (app/current-game-id db)]
    (router/dispatch ev-chan [::clear-selection])
    (game/end-turn! conn game-id)
    (let [game (game/game-by-id @conn game-id)
          cur-faction (:game/current-faction game)]
      (when (:faction/ai cur-faction)
        (players/notify notify-chan [::players/start-turn (:faction/color cur-faction)])
        #_(ai/execute-turn conn game-id)
        #_(game/end-turn! conn game-id)
        ))
    (app/set-url-game-state! @conn)))

(defmethod router/handle-event ::new-game
  [{:as handler-ctx :keys [ev-chan conn]} _]
  (when (js/confirm "Are you sure you want to end your current game and start a new one?")
    (set! js/window.location.hash "")
    (app/start-new-game! handler-ctx :sterlings-aruba-multiplayer)))

(defmethod router/handle-event ::toggle-faction-ai
  [{:as handler-ctx :keys [ev-chan conn db]} [_ faction]]
  (let [{:keys [game/factions]} (app/current-game db)
        {:keys [faction/ai]} faction
        other-factions (remove #(= (e faction) (e %)) factions)]
    (if (and (not ai)
             (= (count other-factions)
                (count (filter :faction/ai other-factions))))
      {:dispatch [[::alert "Enabling AI on all factions is not yet supported."]]}
      {:tx [[:db/add (e faction) :faction/ai (not ai)]]})))

(defmethod router/handle-event ::hide-win-dialog
  [{:as handler-ctx :keys [ev-chan db]} _]
  (let [app (app/root db)]
    {:tx [[:db/add (e app) :app/hide-win-dialog true]]}))
