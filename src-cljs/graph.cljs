(ns graph
  (:refer-clojure :exclude
   [ get get-in vals count subvec
     concat map mapcat reverse
     into-array
     vector hash-map set])
  (:require
    [graph.core :as graph]
    [clojure.string :refer [join]]))

(def ^:export create graph/create)
(def ^:export addVertex graph/addVertex)
(def ^:export lastVertex graph/lastVertex)
(def ^:export getVertex graph/getVertex)
(def ^:export getVertices graph/getVertices)
(def ^:export addEdge graph/addEdge)
(def ^:export getEdges graph/getEdges)
(def ^:export getEdgeTypes graph/getEdgeTypes)
(def ^:export getOutgoing graph/getOutgoing)
(def ^:export getOutgoingRecur graph/getOutgoingRecur)
(def ^:export getAllOutgoing graph/getAllOutgoing)
(def ^:export getIncoming graph/getIncoming)
(def ^:export getIncomingRecur graph/getIncomingRecur)
(def ^:export getAllIncoming graph/getAllIncoming)
(def ^:export expand graph/expand)
(def ^:export sortBy graph/sortBy)
(def ^:export uniq graph/uniq)

(defn ^:export expandWhere
  [graph vertKeys query]
  (let [query (if (map? query) query (js->clj query))]
    (graph/expandWhere graph vertKeys query)))

(defn getKey
  [vertex]
  (join [(aget vertex "ns") ":" (str (aget vertex "id"))]))

(defn addEgdeToIndex
  [index from to]
  (let [vertexIndex (or (cljs.core/get index from) [])]
    (assoc! index from (conj vertexIndex to))))

(defn ^:export fromJson
  [jsonGraph]
  (let [g {}
        jsonVertices (aget jsonGraph "vertices")
        jsonEdges (aget jsonGraph "edgeGroups")
        edgeNames (.keys js/Object jsonEdges)
        vertMap (areduce jsonVertices index acc {}
                  (assoc acc index (getKey (aget jsonVertices index))))
        vertices (transient {})
        edges (transient {})
        indexes (transient {})]
    (-> g
      (assoc "map" vertMap) ; for debug
      (assoc "edgeNames" edgeNames) ; for debug
      (assoc "vertices"
        (persistent!
          (areduce jsonVertices idx acc vertices
            (let [vertex (aget jsonVertices idx)
                  vertKey (getKey vertex)]
              (assoc! acc
                vertKey (assoc (js->clj vertex)
                  "vertexId" vertKey))))))
      (assoc "edges"
        (persistent!
          (areduce edgeNames edgeNameIndex acc edges
            (let [edgeName (aget edgeNames edgeNameIndex)
                  edgeArr (aget jsonEdges edgeName)]
              (assoc! acc edgeName
                (persistent!
                  (areduce edgeArr edgeIdx edges (transient [])
                    (let [edge (aget edgeArr edgeIdx)
                          fromIdx (aget edge 0)
                          toIdx (aget edge 1)]
                      (conj! edges [
                        (cljs.core/get vertMap fromIdx)
                        (cljs.core/get vertMap toIdx)])))))))))
      (assoc "indexes"
        (persistent!
          (areduce edgeNames edgeNameIdx acc indexes
            (let [edgeName (aget edgeNames edgeNameIdx)
                  edgeArr (aget jsonEdges edgeName)]
              (assoc! acc edgeName
                (persistent!
                  (areduce edgeArr edgeIdx index (transient {})
                    (let [edge (aget edgeArr edgeIdx)
                          fromIdx (aget edge 0)
                          toIdx (aget edge 1)]
                      (addEgdeToIndex index
                        (cljs.core/get vertMap fromIdx)
                        (cljs.core/get vertMap toIdx))))))))))
    )
  ))

; taken from mori, will add more as needed
(def ^:export get cljs.core/get)
(def ^:export get-in cljs.core/get-in)
(def ^:export vals cljs.core/vals)
(def ^:export count cljs.core/count)
(def ^:export subvec cljs.core/subvec)
(def ^:export concat cljs.core/concat)
(def ^:export map cljs.core/map)
(def ^:export mapcat cljs.core/mapcat)
(def ^:export reverse cljs.core/reverse)
(def ^:export into-array cljs.core/into-array)

; basic types
(def ^:export vector cljs.core/vector)
(def ^:export hash-map cljs.core/hash-map)
(def ^:export set cljs.core/set)

; type predicates
(def ^:export is-list cljs.core/list?)
(def ^:export is-seq cljs.core/seq?)
(def ^:export is-vector cljs.core/vector?)
(def ^:export is-map cljs.core/map?)
(def ^:export is-set cljs.core/set?)

(def ^:export clj-to-js cljs.core/clj->js)
(defn ^:export js-to-clj
  ([x] (cljs.core/js->clj x))
  ([x keywordize-keys] (cljs.core/js->clj x :keywordize-keys keywordize-keys)))
