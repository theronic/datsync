(def project-name "datsync")
(def project-version "0.0.1-alpha3-SNAPSHOT")               ; todo: use git SHA as version number
(def project-url "https://github.com/metasoarous/datsync")
(def project-description "Datomic <-> DataScript syncing/replication utilities")
;(def dependencies (edn/read-string (slurp "deps.edn")))

;(def boot-deps (get-in dependencies [:aliases :test :extra-deps]))
;(defn- libs->boot-deps
;  [deps artifact info]
;  (if-let [version (:mvn/version info)]
;    (conj deps
;          (transduce cat conj [artifact version]
;                     (select-keys info
;                                  [:scope :exclusions])))
;
;(defn translate-deps
;  "{org/project {:mvn/version \"1.0.1\"} ...}
;   => [[org/project \"1.0.1\"] ...]
;  Takes a map of deps from deps.edn and transl"
;  (select-keys dep [:scope ])
;  (reduce-kv cat conj ))

(set-env!
  :source-paths #{"src/cljc" "src/clj"
                  "test/cljc" "test/clj"
                  "env/dev"}
  ; should infer env source from environment
  :resource-paths #{"resources"}
  :target-path "target"
  ; we can probably load in these deps from deps.edn alias.
  :dependencies '[[seancorfield/boot-tools-deps "0.2.2"]
                  ;; Boot
                  [pandeiro/boot-http "0.8.3"]
                  [adzerk/boot-cljs "2.1.4" :scope "test"]
                  ;
                  [adzerk/boot-cljs-repl "0.3.3"]           ;; latest release
                  [com.cemerick/piggieback "0.2.1" :scope "test"]
                  [weasel "0.7.0" :scope "test"]
                  [org.clojure/tools.nrepl "0.2.12" :scope "test"]
                  ;
                  [adzerk/boot-reload "0.5.2" :scope "test"]
                  ;
                  ;;;; Testing
                  [adzerk/boot-test "1.2.0" :scope "test"]
                  [crisptrutski/boot-cljs-test "0.3.5-SNAPSHOT"]
                  [metosin/boot-alt-test "0.3.2" :scope "test"]

                  ;[nightlight "RELEASE" :scope "test"]
                  ])

(boot.core/load-data-readers!)

;(require '[clojure.tools.namespace.repl])
;(clojure.tools.namespace.repl/set-refresh-dirs "clj")       ; work-around sente incompatibility with clj/cljs target mangling

; todo spec
;(s/check-asserts true)

(require
  '[boot-tools-deps.core :as deps]
  '[adzerk.boot-cljs :refer [cljs]]
  '[adzerk.boot-test]
  ;'[adzerk.boot-cljs-repl :refer [cljs-repl start-repl repl-env]]
  '[adzerk.boot-reload :refer [reload]]
  '[pandeiro.boot-http :refer [serve]]
  '[metosin.boot-alt-test :refer [alt-test]]
  ;'[environ.boot :refer [environ]]
  '[crisptrutski.boot-cljs-test :refer [test-cljs prep-cljs-tests run-cljs-tests]] ; not using atm., but will need
  ;'[system.repl :refer [go reset start stop]]
  ;'[system.boot :refer [system run]]
  )

; Watch boot temp dirs
;(init-ctn!)                                                 ; do we need this?

(task-options!
  pom {:project     project-name
       :version     project-version
       :description project-description
       :url         project-url}
  repl {:eval    '(do (set! *warn-on-reflection* true)
                      (set! *print-length* 20))
        :init-ns 'jasure.core.handler}
  aot {:namespace #{'jasure.core}}                          ; FIXME!
  jar {:main 'datsync.core
       :file "datsync.jar"}
  reload {:on-jsload 'jasure.app/mount}
  cljs {:source-map       false
        :compiler-options {:asset-path    "/js/main.out"
                           :optimizations :none}
        ;:asset-path "/js/main.out"
        ;:pretty-print true
        }                                                   ; don't these work?
  ;less {:source-map true}
  )

(deftask deps []
         (deps/deps :overwrite-boot-deps true :aliases [:test]))

(deftask repl-client []
         (comp (repl :client true)))

(deftask test
         [a autotest bool]
         (comp
           (deps)
           (alt-test)))

(deftask dev
         "Start the dev env..."
         [s speak bool "Notify when build is done"
          p port PORT int "Port for web server"
          a use-sass bool "Use Scss instead of less"
          t test-cljs bool "Compile and run cljs tests"]
         (comp
           (deps)
           (watch)
           (cljs :ids #{"js/main"}
                 :source-map true
                 :compiler-options {:optimizations :none})
           (alt-test)
           (if speak (boot.task.built-in/speak) identity)))

(deftask build-cljs []
         (comp
           (deps)
           (cljs :compiler-options {:optimizations :advanced})))

(deftask build-clj []
         ; why aot? Suspect Heroku
         (comp (deps) (aot) (pom) (uber) (jar)))

(deftask package
         "Build the package"
         []
         (comp
           ;(less :compression true)
           (build-cljs)
           (build-clj)
           (target)))