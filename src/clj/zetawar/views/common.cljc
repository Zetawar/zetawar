(ns zetawar.views.common
  #?@(:clj
      [(:require
        [clojure.java.io :as io]
        [hiccup.page :refer [html5 include-css include-js]]
        [zetawar.site :as site])]
      :cljs
      [(:require
        [cljsjs.react-bootstrap]
        [zetawar.site :as site])]))

#?(:clj
   (do

     (defn ga [tracking-id]
       [:script
        (str "(function(i,s,o,g,r,a,m){i['GoogleAnalyticsObject']=r;i[r]=i[r]||function(){"
             "(i[r].q=i[r].q||[]).push(arguments)},i[r].l=1*new Date();a=s.createElement(o),"
             "m=s.getElementsByTagName(o)[0];a.async=1;a.src=g;m.parentNode.insertBefore(a,m)"
             "})(window,document,'script','https://www.google-analytics.com/analytics.js','ga');"
             "ga('create', '" tracking-id "', 'auto');"
             "ga('send', 'pageview');")])

     (defn sentry [sentry-url environment]
       [[:script {:src "https://cdn.ravenjs.com/3.9.1/raven.min.js"}]
        [:script (str "Raven.config('" sentry-url "', {"
                      "release: '" site/build "',"
                      "environment: '" environment "',"
                      "tags: {git_commit: '" site/build "'}"
                      "}).install();")]])

     (defn head [{global-meta :meta :as data} title]
       (into [:head
              [:meta {:charset "utf-8"}]
              [:meta {:http-equiv "X-UA-Compatible" :content "IE=edge"}]
              [:meta {:name "viewport" :content "width=device-width, initial-scale=1"}]
              [:title title]
              (include-css (site/prefix "/css/main.css"))
              (some-> (:google-analytics-tracking-id global-meta)
                      ga)]
             (some-> (:sentry-url global-meta)
                     (sentry (:sentry-environment global-meta)))))

     ))

(def nav-links
  [{:href (site/prefix "/")                            :title "Game"}
   {:href (site/prefix "/blog")                        :title "Blog"}
   {:href (site/prefix "/docs")                        :title "Documentation"}
   {:href "https://github.com/zetawar/zetawar/issues"  :title "Roadmap"}
   {:href (site/prefix "/backers")                     :title "Backers"}])

(defn navbar
  ([] (navbar nil))
  ([active-title]
   #?(
      :clj
      [:div#navbar-wrapper {:data-active-title active-title}
       [:nav.navbar.navbar-inverse.navbar-fixed-top
        [:div.container
         [:div.navbar-header
          [:a.navbar-brand {:href "/"}
           [:img {:src (site/prefix "/images/navbar-logo.svg")}]
           "Zetawar"]]
         [:div#navbar-collapse.collapse.navbar-collapse
          (into [:ul.nav.navbar-nav ]
                (for [{:keys [href title]} nav-links]
                  (if (= title active-title)
                    [:li {:class "active"} [:a {:href href} title]]
                    [:li [:a {:href href} title]])))]]]]
      :cljs
      [:> js/ReactBootstrap.Navbar {:fixed-top true :inverse true}
       [:> js/ReactBootstrap.Navbar.Header
        [:> js/ReactBootstrap.Navbar.Brand
         [:a {:href "/"}
          [:img {:src (site/prefix "/images/navbar-logo.svg")}]
          "Zetawar"]]
        [:> js/ReactBootstrap.Navbar.Toggle]]
       [:> js/ReactBootstrap.Navbar.Collapse
        (into [:> js/ReactBootstrap.Nav]
              (map-indexed (fn [idx {:keys [href title]}]
                             (let [active (= title active-title)]
                               [:> js/ReactBootstrap.NavItem {:event-key idx :active active :href href}
                                title]))
                           nav-links))]]
      )
   ))

(defn kickstarter-alert []
  [:div.row
   [:div.col-md-12
    [:div.alert.alert-success
     [:strong
      "Zetawar is a work in progress. Follow "
      [:a {:href "https://twitter.com/ZetawarGame"}
       "@ZetawarGame"]
      " and check out the "
      [:a {:href "https://www.kickstarter.com/projects/djwhitt/zetawar/updates"}
       "Zetawar Kickstarter page"]
      " for updates."]]]])

(defn footer []
  [:div.container
   [:div#footer
    [:p
     "Build: "
     (if (not-empty site/build)
       [:a {:href (str "/builds/" site/build)} site/build]
       "DEV")]
    [:p
     "Follow "
     [:a {:href "https://twitter.com/ZetawarGame"} "@ZetawarGame"]
     " for updates. "
     "Questions or comments? Send us some "
     [:a {:href "http://goo.gl/forms/RgTpkCYDBk"} "feedback"]
     "."]
    [:p
     "Copyright 2016 Arugaba LLC, All Rights Reserved."]
    [:p
     "Artwork from "
     [:a {:href "https://github.com/cvincent/elite-command"} "Elite Command"]
     " Copyright 2015 Chris Vincent under "
     [:a {:href "http://creativecommons.org/licenses/by/4.0/"}
      "Creative Commons Attribution 4.0 International License"]]]])
