(defproject graph "0.1.0-SNAPSHOT"
  :description "A simple clojure graph library"
  :url "http://github.com/aearly/clj-graph"
  :license {:name "Eclipse Public License"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :dependencies [
    [org.clojure/clojure "1.6.0"]
    [org.clojure/clojurescript "0.0-2268"]]
  :plugins [[lein-cljsbuild "1.0.3"]]
  :cljsbuild {
    :builds [{
      :id "dist"
      :source-paths ["src-cljs"]
      :compiler {
        :output-to "dist/graph.js"
        :optimizations :advanced
      }}]})
