(ns graph
  (:refer-clojure :exclude
   [ get get-in vals
     concat mapcat
     into-array])
  (:require [graph.core :as graph]))

(def ^:export create graph/create)
(def ^:export addVertex graph/addVertex)
(def ^:export lastVertex graph/lastVertex)
(def ^:export getVertex graph/getVertex)
(def ^:export getVertices graph/getVertices)
(def ^:export addEdge graph/addEdge)
(def ^:export getEdges graph/getEdges)
(def ^:export getOutgoing graph/getOutgoing)
(def ^:export getAllOutgoing graph/getAllOutgoing)
(def ^:export getIncoming graph/getIncoming)
(def ^:export getAllIncoming graph/getAllIncoming)
(def ^:export expand graph/expand)
(def ^:export uniq graph/uniq)


; taken from mori, will add more as needed
(def ^:export get cljs.core/get)
(def ^:export get-in cljs.core/get-in)
(def ^:export vals cljs.core/vals)
(def ^:export concat cljs.core/concat)
(def ^:export mapcat cljs.core/mapcat)
(def ^:export into-array cljs.core/into-array)

(def ^:export clj-to-js cljs.core/clj->js)
(defn ^:export js-to-clj
  ([x] (cljs.core/js->clj x))
  ([x keywordize-keys] (cljs.core/js->clj x :keywordize-keys keywordize-keys)))
