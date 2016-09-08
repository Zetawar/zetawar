(ns zetawar.events
  (:require
    [cljs.core.async :refer [chan close! put!]]
    [cognitect.transit :as transit]
    [datascript.core :as d]
    [goog.crypt.base64 :as base64]
    [posh.core :as posh]
    [reagent.core :as r]
    [taoensso.timbre :as log]
    [zetawar.ai :as ai]
    [zetawar.app :as app]
    [zetawar.db :refer [e qe qes]]
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
  [{:as ev-ctx :keys [db]} [_ q r]]
  (let [[app game] (only (qes '[:find ?a ?g
                                :where
                                [?a :app/game ?g]]
                              db))
        [selected-q selected-r] (app/selected-qr db)
        [targeted-q targeted-r] (app/targeted-qr db)
        unit (game/unit-at db game q r)
        terrain (game/terrain-at db game q r)
        selected-unit (game/unit-at db game selected-q selected-r)
        selected-terrain (game/terrain-at db game selected-q selected-r)
        targeted-unit (game/unit-at db game targeted-q targeted-r)
        targeted-terrain (game/terrain-at db game targeted-q targeted-r)]
    {:tx (cond
           ;; selecting selected tile
           (and (= q selected-q) (= r selected-r))
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
           (and (= q targeted-q) (= r targeted-r))
           [[:db/retract (e app) :app/targeted-q targeted-q]
            [:db/retract (e app) :app/targeted-r targeted-r]]

           ;; selecting in range enemy unit with unit selected
           (and unit
                selected-unit
                (not (game/unit-current? db game unit))
                (and (game/can-attack? db game selected-unit)
                     (game/in-range? db selected-unit unit)))
           [{:db/id (e app)
             :app/targeted-q q
             :app/targeted-r r}]

           ;; selecting valid destination terrain with unit selected
           (and terrain
                selected-unit
                (game/can-move? db game selected-unit)
                (game/valid-destination? db game selected-unit q r))
           [{:db/id (e app)
             :app/targeted-q q
             :app/targeted-r r}]

           ;; selecting friendly unit with unit or terrain selected
           (and unit
                (or selected-unit selected-terrain)
                (or (game/can-move? db game unit)
                    (game/can-attack? db game unit)))
           (cond-> [{:db/id (e app)
                     :app/selected-q q
                     :app/selected-r r}]
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
                     :app/selected-q q
                     :app/selected-r r}]
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
                     :app/selected-q q
                     :app/selected-r r}]
             (and targeted-q targeted-r)
             (->
               (conj [:db/retract (e app) :app/targeted-q targeted-q])
               (conj [:db/retract (e app) :app/targeted-r targeted-r]))))}))

(defmethod router/handle-event ::clear-selection
  [{:as ev-ctx :keys [db]} _]
  (let [app (qe '[:find ?a
                  :where
                  [?a :app/game]]
                db)
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
  [{:as ev-ctx :keys [db]} [_ q r]]
  (let [app (qe '[:find ?a
                  :where
                  [?a :app/game]]
                db)]
    (when (game/current-faction-won? db)
      {:tx [[:db/add (e app) :app/show-win-dialog true]]})))

;; TODO: convert to message based event handler
(defn move [conn ev-chan ev]
  (let [db @conn
        [q1 r1 q2 r2] (first (d/q '[:find ?q1 ?r1 ?q2 ?r2
                                    :where
                                    [?a :app/selected-q ?q1]
                                    [?a :app/selected-r ?r1]
                                    [?a :app/targeted-q ?q2]
                                    [?a :app/targeted-r ?r2] ]
                                  db))]
    (game/move! conn (app/current-game-id db) q1 r1 q2 r2)
    ;; TODO: cleanup
    ;; TODO: make de-select a second event
    (let [db @conn
          app (qe '[:find ?a
                    :where
                    [?a :app/game]]
                  db)
          game (game/game-by-id db (app/current-game-id db))
          unit (game/unit-at db game q2 r2)
          terrain (game/base-at db game q2 r2)]
      (if (or (and (game/can-attack? db game unit)
                   (not-empty (game/enemies-in-range db game unit)))
              (game/can-capture? db game unit terrain))
        (d/transact! conn  [[:db/add (e app) :app/selected-q q2]
                            [:db/add (e app) :app/selected-r r2]
                            [:db/retract (e app) :app/targeted-q q2]
                            [:db/retract (e app) :app/targeted-r r2]])
        (router/dispatch! ev-chan [::clear-selection])))))

(defmethod router/handle-event ::attack-targeted
  [{:as ev-ctx :keys [db]} _]
  (let [[q1 r1 q2 r2] (first (d/q '[:find ?q1 ?r1 ?q2 ?r2
                                    :where
                                    [?a :app/selected-q ?q1]
                                    [?a :app/selected-r ?r1]
                                    [?a :app/targeted-q ?q2]
                                    [?a :app/targeted-r ?r2]]
                                  db))]
    {:tx       (game/attack-tx db (app/current-game db) q1 r1 q2 r2)
     :dispatch [[::clear-selection]
                [::alert-if-win]]}))

(defmethod router/handle-event ::repair-selected
  [{:as ev-ctx :keys [db]} _]
  (let [[q r] (first (d/q '[:find ?q ?r
                            :where
                            [?a :app/selected-q ?q]
                            [?a :app/selected-r ?r]]
                          db))]
    {:tx (game/repair-tx db (app/current-game db) q r)
     :dispatch [[::clear-selection]]}))

(defmethod router/handle-event ::capture-selected
  [{:as ev-ctx :keys [db]} _]
  (let [[q r] (first (d/q '[:find ?q ?r
                            :where
                            [?a :app/selected-q ?q]
                            [?a :app/selected-r ?r]]
                          db))]
    {:tx (game/capture-tx db (app/current-game db) q r)
     :dispatch [[::clear-selection]
                [::alert-if-win]]}))

(defmethod router/handle-event ::build-unit
  [{:as ev-ctx :keys [db]} _]
  (let [[q r] (first (d/q '[:find ?q ?r
                            :where
                            [?a :app/selected-q ?q]
                            [?a :app/selected-r ?r]]
                          db))]
    (spy [q r])
    {:tx (game/build-tx db (app/current-game db) q r :unit-type.id/infantry)
     :dispatch [[::clear-selection]]}))

(defn end-turn [conn ev-chan ev]
  (let [db @conn
        ;; TODO: replace with app/current-game-id
        game-id (oonly (d/q '[:find ?game-id
                              :where
                              [_ :game/id ?game-id]]
                            db))
        game (game/game-by-id db game-id)]
    (router/dispatch! ev-chan [::clear-selection])
    (game/end-turn! conn game-id)
    (when (get-in (game/game-by-id @conn game-id) [:game/current-faction :faction/ai])
      (ai/execute-turn conn game-id)
      (game/end-turn! conn game-id)
      (router/dispatch! ev-chan [::alert-if-win]))
    (app/set-url-game-state! @conn)))

(defn new-game [conn ev-chan ev]
  (when (js/confirm "Are you sure you want to end your current game and start a new one?")
    (set! js/window.location.hash "")
    (app/start-new-game! conn :sterlings-aruba-multiplayer)))

(defn toggle-faction-ai [conn ev-chan faction ev]
  (let [db @conn
        game-id (app/current-game-id db)
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

(defn hide-win-dialog [conn ev-chan ev]
  (let [db @conn
        app (qe '[:find ?a
                  :where
                  [?a :app/game]]
                db)]
    (d/transact! conn [[:db/add (e app) :app/show-win-dialog false]])))
