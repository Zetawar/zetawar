(ns zetawar.site)

(defn viewing-devcards?
  "Returns true if currently viewing a devcards page."
  []
  (re-matches #".*/devcards.*" js/window.location.href))
