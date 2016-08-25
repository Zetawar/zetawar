(ns zetawar.handlers
  (:require
    [cognitect.transit :as transit]
    [datascript.core :as d]
    [goog.crypt.base64 :as base64]
    [posh.core :as posh]
    [reagent.core :as r]
    [zetawar.ai :as ai]
    [zetawar.app :as app]
    [zetawar.db :refer [e qe qes]]
    [zetawar.game :as game]
    [zetawar.util :refer [only oonly spy]]))

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

(defn select [conn q r ev]
  (let [db @conn
        [app game] (only (qes '[:find ?a ?g
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
    (spy [q r])
    (cond
      ;; selecting selected tile
      (and (= q selected-q) (= r selected-r))
      (d/transact! conn (cond-> []
                          (and selected-q selected-r)
                          (->
                            (conj [:db/retract (e app) :app/selected-q selected-q])
                            (conj [:db/retract (e app) :app/selected-r selected-r]))

                          (and targeted-q targeted-r)
                          (->
                            (conj [:db/retract (e app) :app/targeted-q targeted-q])
                            (conj [:db/retract (e app) :app/targeted-r targeted-r]))))

      ;; selecting targeted tile
      (and (= q targeted-q) (= r targeted-r))
      (d/transact! conn [[:db/retract (e app) :app/targeted-q targeted-q]
                         [:db/retract (e app) :app/targeted-r targeted-r]])

      ;; selecting in range enemy unit with unit selected
      (and unit
           selected-unit
           (not (game/unit-current? db game unit))
           (and (game/can-attack? db game selected-unit)
                (game/in-range? db selected-unit unit)))
      (d/transact! conn [{:db/id (e app)
                          :app/targeted-q q
                          :app/targeted-r r}])

      ;; selecting valid destination terrain with unit selected
      (and terrain
           selected-unit
           (game/can-move? db game selected-unit)
           (game/valid-destination? db game selected-unit q r))
      (d/transact! conn [{:db/id (e app)
                          :app/targeted-q q
                          :app/targeted-r r}])

      ;; selecting friendly unit with unit or terrain selected
      (and unit
           (or selected-unit selected-terrain)
           (or (game/can-move? db game unit)
               (game/can-attack? db game unit)))
      (d/transact! conn (cond-> [{:db/id (e app)
                                  :app/selected-q q
                                  :app/selected-r r}]
                          (and targeted-q targeted-r)
                          (->
                            (conj [:db/retract (e app) :app/targeted-q targeted-q])
                            (conj [:db/retract (e app) :app/targeted-r targeted-r]))))

      ;; selecting owned base with no unit selected
      (and terrain
           (not unit)
           (not selected-unit)
           (game/current-base? db game terrain))
      (d/transact! conn (cond-> [{:db/id (e app)
                                  :app/selected-q q
                                  :app/selected-r r}]
                          (and targeted-q targeted-r)
                          (->
                            (conj [:db/retract (e app) :app/targeted-q targeted-q])
                            (conj [:db/retract (e app) :app/targeted-r targeted-r]))))

      ;; selecting unselected friendly unit
      (and unit
           (or (game/can-move? db game unit)
               (and
                 (game/can-attack? db game unit)
                 (not= 0 (count (game/enemies-in-range db game unit))))
               (game/can-capture? db game unit terrain)))
      (d/transact! conn (cond-> [{:db/id (e app)
                                  :app/selected-q q
                                  :app/selected-r r}]
                          (and targeted-q targeted-r)
                          (->
                            (conj [:db/retract (e app) :app/targeted-q targeted-q])
                            (conj [:db/retract (e app) :app/targeted-r targeted-r])))))))

(defn clear-selection [conn ev]
  (let [db @conn
        app (qe '[:find ?a
                  :where
                  [?a :app/game]]
                db)
        [selected-q selected-r] (app/selected-qr db)
        [targeted-q targeted-r] (app/targeted-qr db)]
    (d/transact! conn (cond-> []
                        (and selected-q selected-r)
                        (->
                          (conj [:db/retract (e app) :app/selected-q selected-q])
                          (conj [:db/retract (e app) :app/selected-r selected-r]))

                        (and targeted-q targeted-r)
                        (->
                          (conj [:db/retract (e app) :app/targeted-q targeted-q])
                          (conj [:db/retract (e app) :app/targeted-r targeted-r]))))))

(defn alert-if-win [conn]
  (let [db @conn
        app (qe '[:find ?a
                  :where
                  [?a :app/game]]
                db)]
    (when (game/current-faction-won? db)
      (d/transact! conn [[:db/add (e app) :app/show-win-dialog true]]))))

(defn move [conn ev]
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
        ;; TODO: cleanup clear-selection handler usage
        (clear-selection conn nil)))))

(defn attack [conn ev]
  (let [db @conn
        [q1 r1 q2 r2] (first (d/q '[:find ?q1 ?r1 ?q2 ?r2
                                    :where
                                    [?a :app/selected-q ?q1]
                                    [?a :app/selected-r ?r1]
                                    [?a :app/targeted-q ?q2]
                                    [?a :app/targeted-r ?r2]]
                                  db))]
    (game/attack! conn (app/current-game-id db) q1 r1 q2 r2)
    ;; TODO: cleanup clear-selection handler usage
    (clear-selection conn nil)
    (alert-if-win conn)))

(defn repair [conn ev]
  (let [db @conn
        [q r] (first (d/q '[:find ?q ?r
                            :where
                            [?a :app/selected-q ?q]
                            [?a :app/selected-r ?r]]
                          db))]
    (game/repair! conn (app/current-game-id db) q r)
    ;; TODO: cleanup clear-selection handler usage
    (clear-selection conn nil)))

(defn capture [conn ev]
  (let [db @conn
        [q r] (first (d/q '[:find ?q ?r
                            :where
                            [?a :app/selected-q ?q]
                            [?a :app/selected-r ?r]]
                          db))]
    (game/capture! conn (app/current-game-id db) q r)
    ;; TODO: cleanup clear-selection handler usage
    (clear-selection conn nil)
    (alert-if-win conn)))

(defn build [conn ev]
  (let [db @conn
        [q r]  (first (d/q '[:find ?q ?r
                             :where
                             [?a :app/selected-q ?q]
                             [?a :app/selected-r ?r]]
                           db))]
    (game/build! conn (app/current-game-id db) q r :unit-type.id/infantry)
    ;; TODO: cleanup clear-selection handler usage
    (clear-selection conn nil)))

(defn end-turn [conn ev]
  (let [db @conn
        ;; TODO: replace with app/current-game-id
        game-id (oonly (d/q '[:find ?game-id
                              :where
                              [_ :game/id ?game-id]]
                            db))
        game (game/game-by-id db game-id)]
    ;; TODO: cleanup clear-selection handler usage
    (clear-selection conn nil)
    (game/end-turn! conn game-id)
    (when (get-in (game/game-by-id @conn game-id) [:game/current-faction :faction/ai])
      (ai/execute-turn conn game-id)
      (game/end-turn! conn game-id)
      (alert-if-win conn))
    (app/set-url-game-state! @conn)))

(defn new-game [conn ev]
  (when (js/confirm "Are you sure you want to end your current game and start a new one?")
    (set! js/window.location.hash "")
    (app/start-new-game! conn :sterlings-aruba-multiplayer)))

(defn toggle-faction-ai [conn faction ev]
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

(defn hide-win-dialog [conn ev]
  (let [db @conn
        app (qe '[:find ?a
                  :where
                  [?a :app/game]]
                db)]
    (d/transact! conn [[:db/add (e app) :app/show-win-dialog false]])))
