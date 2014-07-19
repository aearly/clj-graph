(ns graph.core
  (:require
    [clojure.string :refer [join]]))

(def ^:private FROM 0)
(def ^:private TO 1)

(defn vertex-key
  "abstraction for creating vertex keys from several types of arguments"
  ([nom id]
    (join ":" [nom (str id)]))
  ([arg]
    (if (vector? arg)
      (apply vertex-key arg)
      (if (re-find #":" arg)
        arg
        (vertex-key "V" arg)))))


(defn create
  "initializes an empty graph"
  []
  {
    "vertices" {}
    "edges" {}
    "indexes" {}
  })

; Vertex functions

(defn addVertex
  "add a vertex to the graph.  All edges are stored in a single hash map. Each
  vertex is keyed by namespace:id.  The id is coerced to a string"
  ([graph nom data]
    (let [verts (get graph "vertices")
          id (get data "id" (str (count verts))) ;if no id, use the number of verts
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
      {}
      (get graph "vertices"))))


; Edge Functions

(defn- addEdgeToIndex
  "creates hashmaps of edges based on their from vertices"
  [graph nom from to]
  (update-in graph ["indexes" nom from] (fn [index]
    (let [index (or index [])]
      (conj index to)))))

(defn addEdge
  "Add an edge. Edges are stored as lists of [from to] pairs for each namespace.
  The edge lists are stored in hashmap keyed on the namespace."
  [graph nom opts]
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

(defn getEdgeTypes [graph]
  (keys (get graph "edges")))

; TODO: "remove" operations


; Relationship Functions

(defn mapset
  [func coll]
  "Map over a collection, but store the results in a set.  If the map returns
  a vector or sequence, it will be flattened"
  (reduce
    (fn [acc item]
      (let [result (func item)]
        (if (or (vector? result) (seq? result))
          (into acc result)
          (conj acc result))))
    #{}
    coll))

; TODO: make this a macro
(defn createRecurWalker
  [func]
  "Helper method to create the getOutgoingRecur and getIncomingRecur functions.
  Recursively calls getOutgoing until the set of vertex ids stops growing"
  (fn [graph relName vertKeys]
    (let [keys (if (set? vertKeys)
                  vertKeys
                  (if (vector? vertKeys)
                    (into #{} vertKeys)
                    #{vertKeys}))] ; otherwise, scalar
      (vec
        (loop [lastSize -1
               curSet keys]
          (if (= (count curSet) lastSize)
            curSet
            (recur
              (count curSet)
              (into curSet (func graph relName curSet))))))))
  )

(defn getOutgoing
  ([graph relName vertKey]
    (if (or (vector? vertKey) (set? vertKey)) ; support a sequence of ids
      (vec (mapset
        (fn [key]
          (getOutgoing graph relName key))
        vertKey))
      (get-in graph ["indexes" relName vertKey] []))
    )
  ([graph relName nom id]
    (getOutgoing graph relName (vertex-key nom id))))

(def getOutgoingRecur (createRecurWalker getOutgoing))

(defn getAllOutgoing
  ([graph vertKey]
    (vec
      (mapset
        (fn [edgeName]
          (getOutgoing graph edgeName vertKey))
        (keys (get graph "edges")))))
  ([graph nom id]
    (getAllOutgoing graph (vertex-key nom id))))

(defn getIncoming
  ([graph relName vertKey]
    (if (or (vector? vertKey) (set? vertKey)) ; support a sequence of ids
      (vec (mapset
        (fn [key]
          (getIncoming graph relName key))
        vertKey))
      (reduce
        (fn [acc edge]
          (if (= (get edge TO) vertKey)
            (conj acc (get edge FROM))
            acc))
        []
        (get-in graph ["edges" relName]))))
  ([graph relName nom id]
    (getIncoming graph relName (vertex-key nom id))))


(def getIncomingRecur (createRecurWalker getIncoming))

(defn getAllIncoming
  ([graph vertKey]
    (vec (mapset
      (fn [edgeName]
        (getIncoming graph edgeName vertKey))
      (keys (get graph "edges")))))
  ([graph nom id]
    (getAllIncoming graph (vertex-key nom id))))

(defn expand [graph vertKeys]
  (vec
    (map
      (fn [key]
        (get-in graph ["vertices" key]))
      vertKeys)))

(defn matches
  [query]
  (if (= (count query) 1)
    ; common case of a single key/val
    (let [firstKey (first (keys query))
          firstVal (get query firstKey)]
      (fn [item]
        (= (get item firstKey) firstVal)))
    (fn [item]
      (let [numCommon (reduce-kv
                        (fn [cnt key val]
                          (if (= (get item key) val)
                            (+ cnt 1)
                            cnt))
                        0
                        item)]
        (and (> numCommon 0) (= numCommon (count query)))))))

(defn expandWhere [graph vertKeys query]
  (vec
    (filter
      (fn [key]
        (or
          (= (get query "vertexId") key)
          ((matches query) (get-in graph ["vertices" key])))
          )
      vertKeys)))

(defn sortBy [graph vertKeys prop]
  (vec
    (sort-by
      (fn [lkey rkey]
        (let [lhs (get-in graph ["vertices" lkey])
              rhs (get-in graph ["vertices" rkey])]
          (< (get lhs prop) (get rhs prop))))
      vertKeys)))

(defn uniq [coll]
  (vec (into (sorted-set) coll)))
