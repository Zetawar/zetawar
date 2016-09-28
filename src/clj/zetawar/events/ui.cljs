(ns zetawar.events.ui
  (:require
   [datascript.core :as d]
   [taoensso.timbre :as log]
   [zetawar.ai :as ai]
   [zetawar.app :as app]
   [zetawar.db :refer [e qe qes]]
   [zetawar.events.game :as e.game]
   [zetawar.game :as game]
   [zetawar.router :as router]
   [zetawar.util :refer [only oonly spy]])
  (:require-macros
   [cljs.core.async.macros :refer [go go-loop]]))

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
        [selected-q selected-r] (app/selected-qr db)
        [targeted-q targeted-r] (app/targeted-qr db)
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
             (->
              (conj [:db/retract (e app) :app/selected-q selected-q])
              (conj [:db/retract (e app) :app/selected-r selected-r]))

             (and targeted-q targeted-r)
             (->
              (conj [:db/retract (e app) :app/targeted-q targeted-q])
              (conj [:db/retract (e app) :app/targeted-r targeted-r])))

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
              (conj [:db/retract (e app) :app/targeted-q targeted-q])
              (conj [:db/retract (e app) :app/targeted-r targeted-r])))

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
              (conj [:db/retract (e app) :app/targeted-q targeted-q])
              (conj [:db/retract (e app) :app/targeted-r targeted-r])))

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
              (conj [:db/retract (e app) :app/targeted-q targeted-q])
              (conj [:db/retract (e app) :app/targeted-r targeted-r]))))}))

(defmethod router/handle-event ::clear-selection
  [{:as handler-ctx :keys [db]} _]
  (let [app (app/root db)
        [selected-q selected-r] (app/selected-qr db)
        [targeted-q targeted-r] (app/targeted-qr db)]
    {:tx (cond-> []
           (and selected-q selected-r)
           (->
            (conj [:db/retract (e app) :app/selected-q selected-q])
            (conj [:db/retract (e app) :app/selected-r selected-r]))

           (and targeted-q targeted-r)
           (->
            (conj [:db/retract (e app) :app/targeted-q targeted-q])
            (conj [:db/retract (e app) :app/targeted-r targeted-r])))}))

(defmethod router/handle-event ::alert-if-win
  [{:as handler-ctx :keys [db]} _]
  (let [app (app/root db)]
    (when (game/current-faction-won? db)
      {:tx [[:db/add (e app) :app/show-win-dialog true]]})))

(defmethod router/handle-event ::move-selected-unit
  [{:as handler-ctx :keys [db]} _]
  (let [[from-q from-r] (app/selected-qr db)
        [to-q to-r] (app/targeted-qr db)]
    {:dispatch [[::e.game/move-unit from-q from-r to-q to-r]
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
  (let [[attacker-q attacker-r] (app/selected-qr db)
        [target-q target-r] (app/targeted-qr db)]
    {:dispatch [[::e.game/attack-unit attacker-q attacker-r target-q target-r]
                [::clear-selection]
                [::alert-if-win]]}))

(defmethod router/handle-event ::repair-selected
  [{:as handler-ctx :keys [db]} _]
  (let [[q r] (app/selected-qr db)]
    {:dispatch [[::e.game/repair-unit q r]
                [::clear-selection]]}))

(defmethod router/handle-event ::capture-selected
  [{:as handler-ctx :keys [db]} _]
  (let [[q r] (app/selected-qr db)]
    {:dispatch [[::e.game/capture-base q r]
                [::clear-selection]
                [::alert-if-win]]}))

(defmethod router/handle-event ::build-unit
  [{:as handler-ctx :keys [db]} _]
  (let [[q r] (app/selected-qr db)]
    {:dispatch [[::e.game/build-unit q r :unit-type.id/infantry]
                [::clear-selection]]}))

;; TODO: convert to pure function
(defmethod router/handle-event ::end-turn
  [{:as handler-ctx :keys [ev-chan conn db]} _]
  (let [game-id (app/current-game-id db)
        game (game/game-by-id db game-id)]
    (router/dispatch ev-chan [::clear-selection])
    (game/end-turn! conn game-id)
    (when (get-in (game/game-by-id @conn game-id) [:game/current-faction :faction/ai])
      (ai/execute-turn conn game-id)
      (game/end-turn! conn game-id)
      (router/dispatch ev-chan [::alert-if-win]))
    (app/set-url-game-state! @conn)))

;; TODO: convert to pure function
(defmethod router/handle-event ::new-game
  [{:as handler-ctx :keys [ev-chan conn db]} _]
  (when (js/confirm "Are you sure you want to end your current game and start a new one?")
    (set! js/window.location.hash "")
    (app/start-new-game! conn :sterlings-aruba-multiplayer)))

;; TODO: convert to pure function
(defmethod router/handle-event ::toggle-faction-ai
  [{:as handler-ctx :keys [ev-chan conn db]} [_ faction]]
  (let [game-id (app/current-game-id db)
        {:keys [faction/ai]} faction
        other-faction-count (oonly (d/q '[:find (count ?f)
                                          :in $ ?game-id ?f-arg
                                          :where
                                          [?g :game/id ?game-id]
                                          [?g :game/factions ?f]
                                          [(not= ?f ?f-arg)]]
                                        db game-id (e faction)
                                        ))
        other-faction-ai-count (-> (d/q '[:find (count ?f)
                                          :in $ ?game-id ?f-arg
                                          :where
                                          [?g :game/id ?game-id]
                                          [?g :game/factions ?f]
                                          [?f :faction/ai true]
                                          [(not= ?f ?f-arg)]]
                                        db game-id (e faction))
                                   ffirst
                                   (or 0))]
    (if (and (not ai)
             (= other-faction-count other-faction-ai-count))
      (js/alert "Enabling AI on all factions is not yet supported.")
      (d/transact! conn [[:db/add (e faction) :faction/ai (not ai)]]))))

;; TODO: convert to pure function
(defmethod router/handle-event ::hide-win-dialog
  [{:as handler-ctx :keys [ev-chan conn db]} _]
  (let [app (app/root db)]
    (d/transact! conn [[:db/add (e app) :app/show-win-dialog false]])))
