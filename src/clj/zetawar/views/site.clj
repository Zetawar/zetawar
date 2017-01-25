(ns zetawar.views.site
  (:require
   [zetawar.views.common :refer [footer head kickstarter-alert navbar]]
   [hiccup.page :refer [html5 include-js]]
   [zetawar.site :as site])
  (:import
   [java.text SimpleDateFormat]
   [java.util TimeZone]))

(defn date-fmt [date]
  (let [formater (SimpleDateFormat. "MMMM dd, yyyy")]
    (.setTimeZone formater (TimeZone/getTimeZone "GMT"))
    (.format formater date)))

(defn render-index [data]
  (let [{global-meta :meta} data]
    (html5
     (head data (:site-title global-meta))
     [:body#app
      [:div#main
       (navbar "Game")
       [:div.container.text-center
        [:img {:src (site/prefix "/images/spin-64.gif")}]]
       (footer)]
      (include-js (site/prefix "/js/main.js"))])))

(defn render-blog-index [data]
  (let [{:keys [entries] global-meta :meta} data]
    (html5
     (head data (:site-title global-meta))
     [:body#site
      (navbar "Blog")
      [:div.container
       (kickstarter-alert)
       (into [:div#blog-posts]
             (for [entry entries]
               [:div.blog-post
                [:div.blog-post-title (:description entry)]
                [:div.blog-post-meta
                 [:a {:href (:permalink entry)}
                  (date-fmt (:date-published entry))]]
                (:content entry)]))]
      (footer)
      (include-js (site/prefix "/js/main.js"))])))

(defn render-blog-post [data]
  (let [{:keys [entry] global-meta :meta} data]
    (html5
     (head data (:site-title global-meta))
     [:body#site
      (navbar "Blog")
      [:div.container
       (kickstarter-alert)
       [:div.blog-post
        [:div.blog-post-title (:description entry)]
        [:div.blog-post-meta
         (date-fmt (:date-published entry))]
        (:content entry)]]
      (footer)
      (include-js (site/prefix "/js/main.js"))])))

(defn render-page [data]
  (let [{:keys [entry] global-meta :meta} data]
    (html5
     (head data (:site-title global-meta))
     [:body#site
      (navbar (:name entry))
      [:div.container
       (kickstarter-alert)
       (:content entry)]
      (footer)
      (include-js (site/prefix "/js/main.js"))])))

(defn render-devcards [data]
  (let [{:keys [entry] global-meta :meta} data]
    (html5
     (head data "Zetawar Devcards")
     [:body#devcards
      (include-js (site/prefix "/js/main.js"))
      (include-js (site/prefix "/js/main/devcards.js"))])))
