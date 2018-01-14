(ns zetawar.serialization
  (:require
   [cognitect.transit :as transit]
   [goog.crypt.base64 :as base64]
   [lzw]
   [zetawar.app :as app]
   [zetawar.data :as data]
   [zetawar.game :as game]
   [zetawar.logging :as log]
   [zetawar.util :as util :refer [breakpoint inspect]]))

;; TODO: rename ns to encodeing or codecs?

;; TODO: get lzw working in nodejs
;; TODO: move to encoding or serialization ns
;; TODO: document UTF-8 hack

(defn encode-game-state [game-state]
  (let [writer (transit/writer :json)]
    (-> (transit/write writer game-state)
        js/lzwEncode
        js/encodeURIComponent
        js/unescape
        (base64/encodeString true))))

(defn decode-game-state [encoded-game-state]
  (let [reader (transit/reader :json)
        transit-game-state (-> encoded-game-state
                               (base64/decodeString true)
                               js/escape
                               js/decodeURIComponent
                               js/lzwDecode)]
    (transit/read reader transit-game-state)))

;; TODO: move load-encoded-game-state! and set-url-game-state! back to app ns once
;; lzw works on node

(defn load-encoded-game-state!
  ([{:as app-ctx :keys [conn players]} encoded-game-state]
   (load-encoded-game-state! app-ctx data/rulesets data/maps data/scenarios encoded-game-state))
  ([{:as app-ctx :keys [conn players]} rulesets map-defs scenario-defs encoded-game-state]
   (->> encoded-game-state
        decode-game-state
        (app/load-game-state! app-ctx rulesets map-defs scenario-defs))))

;; TODO: put URL in paste buffer automatically
(defn set-url-game-state! [db]
  (let [encoded-game-state (->> (app/current-game db)
                                (game/get-game-state db)
                                encode-game-state)]
    (set! js/window.location.hash encoded-game-state)))
