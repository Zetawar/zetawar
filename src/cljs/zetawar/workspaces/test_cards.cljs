(ns zetawar.workspaces.test-cards
  (:require
   ["react" :as react]
   [nubank.workspaces.card-types.react :as ct.react]
   [nubank.workspaces.core :as ws]))

;; simple function to create react elemnents
(defn element [name props & children]
  (apply react/createElement name (clj->js props) children))

(ws/defcard hello-card
  (ct.react/react-card
   (element "div" {} "Hello World")))
