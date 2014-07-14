(ns clj-graph.core
  (:require
    [clojure.core :refer :all]
    [clojure.string :refer [join]]))

(def ^:private re-colon (re-pattern ":"))

(defn vertex-key
  "abstraction for creating vertex keys from several types of arguments"
  ([nom id]
    (join ":" [nom (str id)]))
  ([arg]
    (if (vector? arg)
      (apply vertex-key arg)
      (if (re-find re-colon arg)
        arg
        (vertex-key "V" arg)))))


(defn create
  "initializes an empty graph"
  [] {
    "vertices" {}
    "edges" {}
    "indexes" {}
  })

; Vertex functions

(defn addVertex
  "add a vertex to the graph"
  ([graph nom data]
    (let [verts (get graph "vertices")
          id (or (get data "id") (str (count verts))) ;if no id, use the number of verts
          data  (assoc data "id" id)] ; make sure id is included on vertex
      (assoc graph
        "vertices" (assoc verts
          (vertex-key nom id) data)))
    )
  ([graph data]
    "default value for the name is 'V'"
    (addVertex graph "V" data)))

(defn lastVertex
  "get the last added vertex"
  [graph]
  (get (last (get graph "vertices")) 1))

(defn getVertex
  "get a vertex by key, or by namespace and id"
  ([graph key]
    (get-in graph ["vertices" key]))
  ([graph nom id]
    (getVertex graph (vertex-key nom id))))

(defn getVertices
  "get all vertices from a given namespace"
  [graph nom]
  (let [re (re-pattern (join ["^" nom ":"]))]
    (reduce-kv
      (fn [verts key vertex]
        (if (re-find re key)
          (assoc verts key vertex)
          verts))
      {} (get graph "vertices"))))


; Edge Functions

(defn- addEdgeToIndex
  "creates hashmaps of edges based on their from vertices"
  [graph nom from to]
  (update-in graph ["indexes" nom from] (fn [index]
    (let [index (or index [])]
      (conj index to)))))

(defn addEdge [graph nom opts]
  (let [from (get opts "from")
        to (get opts "to")]
    (assert (and
      (some? from)
      (some? to)) "invalid options passed to edge")
    (let [from (vertex-key from)
          to (vertex-key to)]
      (assert
        (and (some? (get-in graph ["vertices" from]))
          (some? (get-in graph ["vertices" to])))
          "no such vertex")
      (-> graph
        (update-in ["edges" nom] (fn [edges]
          (let [edges (or edges [])]
            (conj edges [from to]))))
        (addEdgeToIndex nom from to)))))

(defn getEdges [graph nom]
  (get-in graph ["edges" nom]))
