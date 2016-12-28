(ns zetawar.site
  (:require
   [clojure.string :as string])
  #?(:cljs
     (:require-macros
      [zetawar.site :refer [get-site-prefix]])))

#?(:clj

   (defmacro get-site-prefix []
     (or (System/getenv "ZETAWAR_SITE_PREFIX") ""))

   )

(def site-prefix (get-site-prefix))

(defn prefix-url [url]
  (str site-prefix url))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Perun

#?(:clj
   (do

     (defn slug-fn [filename]
       (let [[year month day & parts] (string/split filename #"[-\.]")
             name-part (some->> parts
                                drop-last
                                not-empty
                                (string/join "-")
                                string/lower-case)]
         (when (and year month day name-part)
           (str year "/" month "/" day "/" name-part))))

     (defn permalink-fn [{:keys [slug path filename] :as data}]
       (if (string/starts-with? path "posts")
         (str site-prefix "/blog/" slug "/")
         (str site-prefix (string/replace filename #"\.markdown" "/"))))

     (defn devcards? [{:keys [path]}]
       (= path "pages/devcards.markdown"))

     (defn page? [{:keys [path]}]
       (and (not (devcards? path))
            (string/starts-with? path "pages/")))

     (defn post? [{:keys [path]}]
       (and (not (devcards? path))
            (string/starts-with? path "posts/")))

     )
   )

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;;; Devcards

#?(:cljs
   (do

     (defn viewing-devcards?
       "Returns true if currently viewing devcards."
       []
       (re-matches #".*/devcards.*" js/window.location.href))

     )
   )
