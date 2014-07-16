(defproject graph "0.1.0-SNAPSHOT"
  :description "A simple clojure graph library"
  :url "http://github.com/aearly/clj-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/clojurescript "0.0-2268"]]
  :plugins [[lein-cljsbuild "1.0.3"]
            [com.keminglabs/cljx "0.4.0"]]

  :main graph.core

  ; make sure we run the cljx compiler before tests
  :prep-tasks ["cljx" "javac" "compile"]

  :test-paths ["target/generated/clj" "test"]
  :cljx {:builds [{:source-paths ["src"]
                   :output-path "target/generated/cljs"
                   :rules :cljs}
                  {:source-paths ["src"]
                   :output-path "target/generated/clj"
                   :rules :clj}]}
  :cljsbuild {
    :builds [{
      :id "dist"
      :source-paths ["src-cljs" "target/generated/cljs"]
      :compiler {
        :output-to "dist/graph.js"
        :optimizations :advanced
        :output-wrapper false
      }}]})
