(ns zetawar.players
  (:require
   [cljs.core.async :as async]
   [zetawar.data :as data]
   [zetawar.game :as game]
   [zetawar.logging :as log]))

;; TODO: move to data ns?
;; TODO: namespace keys?
(def player-types [[::human        {:description "Human"
                                    :ai          false}]
                   [::reference-ai {:description "Reference AI"
                                    :ai          true}]
                   [::custom-ai    {:description "Custom AI"
                                    :ai          true}]
                   [::custom-js-ai {:description "Custom JavaScript AI"
                                    :ai          true}]])

(def player-types-by-id
  (into {} player-types))

(defprotocol Player
  (start [player])
  (stop [player]))

(defmulti new-player (fn [player-ctx player-type faction-color] player-type))

(defn notify [notify-chan msg]
  (log/debug "Notifying player:" (pr-str msg))
  (async/put! notify-chan msg))

(defn load-player-game-state! [conn game-state]
  (game/load-game-state! conn
                         data/rulesets
                         data/maps
                         data/scenarios
                         game-state))

;; requests
;; - get-state
;; - move
;; - attack
;; - capture
;; - build
;; - end-turn

;; events
;; - start-turn
;; - game-state

;; events structure
;; - event-type

;; request response notification
;; - request-id
;; - response or error
