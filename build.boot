(set-env!
 :source-paths #{"src/clj" "src/js" "src/scss" "test/clj" "site"}
 :resource-paths #{"assets"}
 :dependencies
 '[;; Dev
   [adzerk/boot-cljs "1.7.228-2" :scope "test"]
   [adzerk/boot-cljs-repl "0.3.3" :scope "test"]
   [adzerk/boot-reload "0.4.13" :scope "test"]
   [binaryage/devtools "0.8.3" :scope "test"]
   [boot-codox "0.10.1" :scope "test"]
   [com.cemerick/piggieback "0.2.1" :scope "test"]
   [crisptrutski/boot-cljs-test "0.2.2" :scope "test"]
   [deraen/boot-sass "0.3.0" :scope "test"]
   [devcards "0.2.2" :scope "test"]
   [nightlight "1.3.2" :scope "test"]
   [org.clojure/tools.nrepl "0.2.12" :scope "test"]
   [org.martinklepsch/boot-gzip "0.1.3" :scope "test"]
   [org.slf4j/slf4j-nop "1.7.21" :scope "test"]
   [pandeiro/boot-http "0.7.6" :scope "test"]
   [perun "0.3.0" :scope "test"]
   [weasel "0.7.0" :scope "test"]

   ;; App
   [cljsjs/clipboard "1.5.9-0"]
   [cljsjs/react "15.3.1-0"]
   [cljsjs/react-bootstrap "0.30.6-0"]
   [cljsjs/react-dom "15.3.1-0"]
   [cljsjs/react-dom-server "15.3.1-0"]
   [com.cognitect/transit-cljs "0.8.239"]
   [com.gfredericks/test.chuck "0.2.7"]
   [com.stuartsierra/component "0.3.1"]
   [com.taoensso/timbre "4.7.4"]
   [danielsz/boot-autoprefixer "0.0.9"]
   [datascript "0.15.4"]
   [hiccup "1.0.5"]
   [org.clojure/clojure "1.9.0-alpha14"]
   [org.clojure/clojurescript "1.9.293"]
   [org.clojure/core.async "0.2.395"]
   [org.clojure/test.check "0.9.0"]
   [org.webjars/bootstrap-sass "3.3.7"]
   [org.webjars/font-awesome "4.7.0"]
   [posh "0.5.5"]
   [reagent "0.6.0"]])

(require
 '[adzerk.boot-cljs :refer :all]
 '[adzerk.boot-cljs-repl :refer :all]
 '[adzerk.boot-reload :refer :all]
 '[clojure.edn :as edn]
 '[clojure.string :as string]
 '[codox.boot :refer [codox]]
 '[crisptrutski.boot-cljs-test :refer [test-cljs]]
 '[danielsz.autoprefixer :refer [autoprefixer]]
 '[deraen.boot-sass :refer :all]
 '[io.perun :refer :all]
 '[nightlight.boot :refer [nightlight]]
 '[org.martinklepsch.boot-gzip :refer [gzip]]
 '[pandeiro.boot-http :refer :all]
 '[zetawar.site])

(task-options!
 test-cljs    {:js-env :phantom}
 autoprefixer {:files ["main.css"]
               :browsers "> 5%"})

(deftask build-css
  "Build Zetawar CSS."
  []
  (comp
   (sift :add-jar {'org.webjars/bootstrap-sass #"META-INF/resources/webjars/bootstrap-sass/3\.3\.7/stylesheets/.*\.scss$"
                   'org.webjars/font-awesome #"META-INF/resources/webjars/font-awesome/4\.7\.0/(fonts|scss/.*\.scss)"})
   (sift :move {#"META-INF/resources/webjars/bootstrap-sass/3\.3\.7/stylesheets" "bootstrap"
                #"META-INF/resources/webjars/font-awesome/4\.7\.0/fonts" "fonts"
                #"META-INF/resources/webjars/font-awesome/4\.7\.0/scss/(_.*\.scss)" "font-awesome/$1"
                #"META-INF/resources/webjars/font-awesome/4\.7\.0/scss/font-awesome.scss" "font-awesome/_font-awesome.scss"})
   (sift :to-source #{#"bootstrap" #"font-awesome"}
         :to-resource #{#"fonts"})
   (sass :options {:precision 8})
   (autoprefixer :exec-path "node_modules/.bin/postcss")
   (sift :move {#"^main.css$" "css/main.css"})))

(deftask build-html
  "Build Zetawar HTML."
  [m metadata-file FILE str]
  (comp (global-metadata :filename metadata-file)
        (markdown)
        (slug :slug-fn zetawar.site/slug-fn)
        (permalink :permalink-fn zetawar.site/permalink-fn)
        (render :renderer 'zetawar.views.site/render-page
                :filterer zetawar.site/page?
                :out-dir ".")
        (render :renderer 'zetawar.views.site/render-blog-post
                :filterer zetawar.site/post?
                :out-dir ".")
        (render :renderer 'zetawar.views.site/render-devcards
                :filterer zetawar.site/devcards?
                :out-dir ".")
        (collection :renderer 'zetawar.views.site/render-index
                    :out-dir "."
                    :page "index.html")
        (collection :renderer 'zetawar.views.site/render-blog-index
                    :filterer zetawar.site/post?
                    :out-dir "."
                    :page "blog/index.html")))

(deftask build-cljs
  "Build Zetawar ClojureScript for deployment."
  []
  (cljs :ids ["js/main"]
        :optimizations :advanced
        :source-map true
        :compiler-options {:parallel-build true}))

(deftask build-site
  "Build Zetawar site for deployment."
  [e environment ENV  str "Perun environment"
   t target-dir  PATH str "Target directory"]
  (comp (build-cljs)
        (build-html :metadata-file (str "perun.base." environment ".edn"))
        (build-css)
        (gzip :regex #{#"\.html$" #"\.css$" #"\.js$"})
        ;; Fileset gets confused without move to *.orig
        (sift :move {#"^(.*)\.html$" "$1.html.orig"
                     #"^(.*)\.css$" "$1.css.orig"
                     #"^(.*)\.js$" "$1.js.orig"})
        (sift :to-source #{#"\.orig$"})
        (sift :move {#"^(.*)\.html\.gz$" "$1.html"
                     #"^(.*)\.css\.gz$" "$1.css"
                     #"^(.*)\.js\.gz$" "$1.js"})
        (codox :name "Zetawar" :language :clojurescript)
        (target :dir (when target-dir #{target-dir}))))

(deftask dev
  "Run Zetawar dev environment."
  [_ reload-host    HOST str "Reload WebSocket host"
   _ reload-port    PORT int "Reload WebSocket port"
   _ cljs-repl-host HOST str "ClojureScript REPL host"
   _ cljs-repl-port PORT int "ClojureScript REPL port"]
  (comp (serve)
        (repl)
        (watch)
        (build-html :metadata-file "perun.base.dev.edn")
        (build-css)
        (reload :on-jsload 'zetawar.core/run
                :cljs-asset-path ""
                :ws-host (or reload-host
                             (System/getenv "ZETAWAR_RELOAD_HOST")
                             (System/getenv "ZETAWAR_DEV_HOST"))
                :ws-port (or reload-host
                             (System/getenv "ZETAWAR_RELOAD_PORT")))
        (cljs-repl-env :ws-host (or reload-host
                                    (System/getenv "ZETAWAR_CLJS_REPL_HOST")
                                    (System/getenv "ZETAWAR_DEV_HOST"))
                       :port (or reload-host
                                 (System/getenv "ZETAWAR_CLJS_REPL_PORT")))
        (cljs :ids ["js/main"]
              :optimizations :none
              :compiler-options {:devcards true
                                 :preloads '[zetawar.dev]
                                 :parallel-build true})
        (target)
        (nightlight :port 4000 :url "http://localhost:3000")))

(deftask run-tests
  "Run Zetawar tests."
  []
  (test-cljs :exit? true
             :cljs-opts {:externs ["js/externs.js"]
                         :foreign-libs [{:file "lzw.js"
                                         :provides ["lzw"]}]}))

(deftask serve-target
  "Serve files in target (useful for checking builds)."
  []
  (comp (serve :dir "target")
        (wait)))
