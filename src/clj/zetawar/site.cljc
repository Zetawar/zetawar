(ns zetawar.site
  (:require
   [clojure.string :as string])
  #?(:cljs
     (:require-macros
      [zetawar.site :refer [env-prefix]])))

#?(:clj

   (defmacro env-prefix []
     (or (System/getenv "ZETAWAR_SITE_PREFIX") ""))

   )

(def +prefix+ (env-prefix))

(defn prefix [& url-parts]
  (apply str +prefix+ url-parts))

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
         (str +prefix+ "/blog/" slug "/")
         (str +prefix+ (string/replace filename #"\.markdown" "/"))))

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
