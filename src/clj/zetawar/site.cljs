(ns zetawar.site)

(defn viewing-devcards?
  "Returns true if currently viewing devcards."
  []
  (re-matches #".*/devcards.*" js/window.location.href))
