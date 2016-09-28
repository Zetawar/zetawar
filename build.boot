(set-env!
 :source-paths #{"src/clj" "src/js" "src/less" "test" "site"}
 :resource-paths #{"assets"}
 :dependencies
 '[[adzerk/boot-cljs "1.7.228-1" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.4.12" :scope "test"]
   [adzerk/boot-test "1.1.2" :scope "test"]
   [binaryage/devtools "0.8.2" :scope "test"]
   [cljsjs/clipboard "1.5.9-0"]
   [cljsjs/react "15.3.1-0"]
   [cljsjs/react-bootstrap "0.29.2-0"]
   [cljsjs/react-dom "15.3.1-0"]
   [cljsjs/react-dom-server "15.3.1-0"]
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [com.cognitect/transit-cljs "0.8.239"]
   [com.rpl/specter "0.12.0"]
   [com.stuartsierra/component "0.3.1"]
   [com.taoensso/timbre "4.7.4"]
   [crisptrutski/boot-cljs-test "0.2.2-SNAPSHOT" :scope "test"]
   [datascript "0.15.4"]
   [deraen/boot-less "0.5.0" :scope "test"]
   [devcards "0.2.1-7" :scope "test"]
   [fipp "0.6.6" :scope "test"]
   [hashobject/boot-s3 "0.1.2-SNAPSHOT" :scope "test"]
   [hiccup "1.0.5"]
   [it.frbracch/boot-marginalia "0.1.3-1" :scope "test"]
   [org.clojure/clojure "1.9.0-alpha13"]
   [org.clojure/clojurescript "1.9.225"]
   [org.clojure/core.async "0.2.391"]
   [org.clojure/test.check "0.9.0"]
   [org.clojure/test.check "0.9.0"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [org.martinklepsch/boot-gzip "0.1.2" :scope "test"]
   [org.webjars.npm/bootswatch "3.3.6"]
   [org.webjars/bootstrap "3.3.6"]
   [org.webjars/font-awesome "4.6.3"]
   [pandeiro/boot-http "0.7.3" :scope "test"]
   [perun "0.3.0" :scope "test"]
   [posh "0.3.5"]
   [reagent "0.6.0"]
   [weasel "0.7.0" :scope "test"]])

(require
 '[adzerk.boot-cljs :refer :all]
 '[adzerk.boot-cljs-repl :refer :all]
 '[adzerk.boot-reload :refer :all]
 '[adzerk.boot-test :refer :all]
 '[clojure.edn :as edn]
 '[clojure.string :as string]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[deraen.boot-less :refer :all]
 '[hashobject.boot-s3 :refer :all]
 '[io.perun :refer :all]
 '[it.frbracch.boot-marginalia :refer [marginalia]]
 '[org.martinklepsch.boot-gzip :refer [gzip]]
 '[pandeiro.boot-http :refer :all])

(task-options!
 test-cljs {:js-env :phantom})

(deftask build-css
  []
  (comp
   (sift :add-jar {'org.webjars/bootstrap #"META-INF/resources/webjars/bootstrap/3\.3\.6/less/.*\.less$"
                   'org.webjars/font-awesome #"META-INF/resources/webjars/font-awesome/4\.6\.3/(fonts|less/.*\.less)"})
   (sift :move {#"META-INF/resources/webjars/bootstrap/3\.3\.6/less" "bootstrap"
                #"META-INF/resources/webjars/font-awesome/4\.6\.3/fonts" "fonts"
                #"META-INF/resources/webjars/font-awesome/4\.6\.3/less" "font-awesome"})
   (sift :to-source #{#"bootstrap" #"font-awesome"})
   (sift :to-resource #{#"fonts"})
   (less)
   (sift :move {#"^main.css$" "css/main.css"})))

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
    (str "/blog/" slug "/")
    (str (string/replace filename #"\.markdown" "/"))))

(defn devcards? [{:keys [path]}]
  (= path "pages/devcards.markdown"))

(defn page? [{:keys [path]}]
  (and (not (devcards? path))
       (string/starts-with? path "pages/")))

(defn post? [{:keys [path]}]
  (and (not (devcards? path))
       (string/starts-with? path "posts/")))

(deftask build-html
  "Build HTML."
  [m metadata-file FILE str]
  (comp (global-metadata :filename metadata-file)
        (markdown)
        (slug :slug-fn slug-fn)
        (permalink :permalink-fn permalink-fn)
        (render :renderer 'zetawar.views.site/render-page
                :filterer page?
                :out-dir ".")
        (render :renderer 'zetawar.views.site/render-blog-post
                :filterer post?
                :out-dir ".")
        (render :renderer 'zetawar.views.site/render-devcards
                :filterer devcards?
                :out-dir ".")
        (collection :renderer 'zetawar.views.site/render-index
                    :out-dir "."
                    :page "index.html")
        (collection :renderer 'zetawar.views.site/render-blog-index
                    :filterer post?
                    :out-dir "."
                    :page "blog/index.html")))

(deftask dev
  "Run dev environment."
  [H host HOST str]
  (comp (serve)
        (repl)
        (watch)
        (test)
        (build-html :metadata-file "perun.base.dev.edn")
        (build-css)
        (reload :on-jsload 'zetawar.core/run
                :cljs-asset-path ""
                :ws-host host)
        (cljs-repl-env)
        (cljs :ids ["js/main"]
              :optimizations :none
              :source-map true
              :compiler-options {:devcards true
                                 :preloads '[zetawar.dev]
                                 :parallel-build true})
        (target)))

(deftask ci
  "Run CI tests."
  []
  (test-cljs :exit? true
             :cljs-opts {:externs ["js/externs.js"]
                         :foreign-libs [{:file "lzw.js"
                                         :provides ["lzw"]}]}))

(deftask serve-target
  "Serve files in target directory."
  []
  (comp (serve :dir "target")
        (wait)))

(deftask build-cljs
  "Build ClojureScript for production and staging deployments."
  []
  (cljs :ids ["js/main"]
        :optimizations :advanced
        :source-map true
        :compiler-options {:parallel-build true}))

(deftask build
  "Build Zetawar."
  [e environment ENV str]
  (comp (build-cljs)
        (build-html :metadata-file (str "perun.base." environment ".edn"))
        (build-css)
        (marginalia)
        (gzip :regex #{#"\.html$" #"\.css$" #"\.js$"})
        (sift :move {#"^(.*)\.html$" "$1.html.orig"
                     #"^(.*)\.css$" "$1.css.orig"
                     #"^(.*)\.js$" "$1.js.orig"})
        (sift :to-source #{#"\.orig$"})
        (sift :move {#"^(.*)\.html\.gz$" "$1.html"
                     #"^(.*)\.css\.gz$" "$1.css"
                     #"^(.*)\.js\.gz$" "$1.js"})
        (target)))
