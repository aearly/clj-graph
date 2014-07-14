(ns clj-graph.core
  (:require
    [clojure.core :refer :all]
    [clojure.string :refer [join]]))

(def -re-colon (re-pattern ":"))

(defn vertex-key
  "abstraction for creating vertex keys from several types of arguments"
  ([nom id]
    (join ":" [nom (str id)]))
  ([arg]
    (if (vector? arg)
      (apply vertex-key arg)
      (if (re-find -re-colon arg)
        arg
        (vertex-key "V" arg)))))


(defn create []
  "initializes an empty graph"
  {
    "vertices" {}
    "edges" {}
    "indexes" {}
  })

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

(defn lastVertex [graph]
  (get (last (get graph "vertices")) 1))

(defn getVertex
  ([graph key]
    (get-in graph ["vertices" key]))
  ([graph nom id]
    (getVertex graph (vertex-key nom id))))
