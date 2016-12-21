(ns zetawar.system.spec
  (:require
   [cljs.core.async :as async]
   [cljs.core.async.impl.protocols :as async.protocols]
   [clojure.spec :as s]
   [clojure.spec.impl.gen :as gen]
   [clojure.test.check]
   [datascript.core :as d]))

;; Logger
(s/def :zetawar.system/logger nil?)

;; DataScript
(s/def :zetawar.system.datascript/schema
  (s/with-gen map?
    #(gen/return {})))

(s/def :zetawar.system.datascript/conn
  (s/with-gen d/conn?
    #(gen/return (d/create-conn {}))))

(s/def :zetawar.system/datascript
  (s/keys :req-un [:zetawar.system.datascript/schema
                   :zetawar.system.datascript/conn]))

;; Players
(s/def :zetawar.system/players
  (s/with-gen (s/and #(satisfies? IDeref %)
                     #(map? (deref %)))
    #(gen/fmap (fn [m] (atom m))
               (gen/map (gen/keyword) (gen/any-printable)))))

;; Router
(s/def :zetawar.system.router/ev-chan
  (s/with-gen
    (s/and #(satisfies? async.protocols/ReadPort %)
           #(satisfies? async.protocols/WritePort %))
    #(gen/return (async/chan))))

(s/def :zetawar.system.router/notify-chan
  (s/with-gen
    #(satisfies? async.protocols/WritePort %)
    #(gen/return (async/chan))))

(s/def :zetawar.system.router/notify-pub
  (s/with-gen
    #(satisfies? async/Pub %)
    #(gen/return (let [notify-chan (async/chan)]
                   (async/pub notify-chan (fn [x] (nth x 1)))))))

(s/def :zetawar.system/router
  (s/keys :req-un [:zetawar.system.datascript/conn
                   :zetawar.system/players
                   :zetawar.system.router/ev-chan
                   :zetawar.system.router/notify-chan
                   :zetawar.system.router/notify-pub]))

;; Views
(s/def :zetawar.system.views/locale string?)

;; TODO: add spec for dispatch function
;; TODO: add spec for translate function

(s/def :zetawar.system/views
  (s/keys :req-un [:zetawar.system.datascript/conn
                   :zetawar.system.router/ev-chan
                   :zetawar.system.views/locale]))

;; System
(s/def :zetawar/system
  (s/keys :req-un [:zetawar.system/logger
                   :zetawar.system/datascript
                   :zetawar.system/players
                   :zetawar.system/router
                   :zetawar.system/views]))
