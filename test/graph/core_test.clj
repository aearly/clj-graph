(ns graph.core-test
  (:require [clojure.test :refer :all]
            [graph.core :refer :all]))

(defn basicGraph []
  (let [g (create)
        v1 {"id" 1 "data" "foo"}
        v2 {"id" 2 "data" "bar"}
        v3 {"id" 3 "data" "baz"}]
    (-> g
      (addVertex "ns" v1)
      (addVertex "ns" v2)
      (addVertex "ns" v3)
      (addEdge "rel" {"from" ["ns" 1] "to" "ns:2"})
      (addEdge "rel" {"from" ["ns" 2] "to" "ns:1"})
      (addEdge "otherrel" {"from" ["ns" 2] "to" "ns:2"}))))


(deftest graph-tests
  (testing "vertices"
    (testing "should create an empty graph"
      (let [g (create)]
        (is (map? g))
        (is (map? (get g "vertices")))
        (is (map? (get g "edges")))))

    (testing "should add a named vertex"
      (let [g (-> (create)
                (addVertex "foo" {"id" 1 "data" "foo"})
              )]
        (is (= (get g "vertices") {"foo:1" {"id" 1 "data" "foo"}}))))

    (testing "should add an id if not included"
      (let [g (addVertex (create) "foo" {})]
        (is (= (lastVertex g) {"id" "0"}))))

    (testing "should get a vertex with metadata"
      (let [vertex {"id" 1 "data" "foo"}
            g (addVertex (create) "foo" vertex)
            v (getVertex g "foo" "1")]
        (is (= v vertex))))

    (testing "should get all vertices with a name"
      (let [g (-> (create)
                (addVertex "ns" {})
                (addVertex "asdf" {})
                (addVertex "ns" {}))
            result (getVertices g "ns")]
        (is (= result {
          "ns:0" {"id" "0"}
          "ns:2" {"id" "2"}
        }))))
  )


  (testing "edges"
    (testing "should add a named edge"
      (let [g (basicGraph)]
        (is (= (get g "edges") {
          "rel" [["ns:1" "ns:2"] ["ns:2" "ns:1"]]
          "otherrel" [["ns:2" "ns:2"]]
          }))))

    (testing "should maintain an index of outgoing verts by edge type"
      (let [g (basicGraph)
            indexes (get g "indexes")]
        (is (= indexes {
          "rel" {
            "ns:1" ["ns:2"]
            "ns:2" ["ns:1"] }
          "otherrel" { "ns:2" ["ns:2"] }
        }))))

    (testing "should get a list of named edges"
      (let [g (basicGraph)
            result (getEdges g "rel")]
        (is (= result [
          ["ns:1" "ns:2"]
          ["ns:2" "ns:1"]
        ]))))

    (testing "should throw when connecting to a nonexistent node"
      (let [g (create)]
        (is (thrown-with-msg? AssertionError #"no such vertex"
          (addEdge g "rel" {"from" "ns:1" "to" "ns:2"})))))
  )


  (testing "relationships"
    (testing "should get verts connected by named outgoing edges"
      (let [g (basicGraph)]
        (is (= (getOutgoing g "rel" "ns" "1") ["ns:2"]))
        (is (= (getOutgoing g "rel" "ns:2") ["ns:1"]))))

    (testing "should get outgoing edges from an array of vertices"
      (let [g (basicGraph)]
        (is (=
          (getOutgoing g "rel" ["ns:1" "ns:2"])
          ["ns:1" "ns:2"]))))

    (testing "should get outgoing edges recursively"
      (let [g (-> (basicGraph)
                (addEdge "rel" {"from" "ns:2" "to" "ns:3"}))]
        (is (= (getOutgoingRecur g "rel" "ns:1")
               ["ns:1" "ns:2" "ns:3"]))))

    (testing "should get outgoing edges recursively (single)"
      (let [g (-> (basicGraph)
                (addVertex "ns" {"id" 4 "data" "qux"})
                (addEdge "rel" {"from" "ns:3" "to" "ns:4"}))]
        (is (= (getOutgoingRecur g "rel" "ns:3")
               ["ns:4"]))))

    (testing "should get outgoing edges recursively (multi, single depth)"
      (let [g (-> (basicGraph)
                (addVertex "ns" {"id" 4 "data" "qux"})
                (addEdge "test" {"from" "ns:3" "to" "ns:4"})
                (addEdge "test" {"from" "ns:3" "to" "ns:1"}))]
        (is (= (getOutgoingRecur g "test" ["ns:3"])
               ["ns:1" "ns:4"]))))

    (testing "should get outgoing edges recursively (self)"
      (let [g (basicGraph)]
        (is (= (getOutgoingRecur g "otherrel" "ns:2")
               ["ns:2"]))))

    (testing "should get verts connected by all outgoing edges"
      (let [g (basicGraph)]
        (is (= (getAllOutgoing g "ns:2") ["ns:1" "ns:2"]))))

    (testing "should get verts connected by named incoming edges"
      (let [g (basicGraph)]
        (is (= (getIncoming g "rel" "ns" "1") ["ns:2"]))
        (is (= (getIncoming g "rel" "ns:2") ["ns:1"]))))

    (testing "should get incoming edges from an array of vertices"
      (let [g (basicGraph)]
        (is (=
          (getIncoming g "rel" ["ns:1" "ns:2"])
          ["ns:1" "ns:2"]))))

    (testing "should get verts connected by all incoming edges"
      (let [g (basicGraph)]
        (is (= (getAllIncoming g "ns:2") ["ns:1" "ns:2"]))))

    (testing "should expand edge results"
      (let [g (basicGraph)
            result (getAllOutgoing g "ns:2")]
        (is (= (expand g result) [
          {"id" 1 "data" "foo"}
          {"id" 2 "data" "bar"}
        ]))))
  )
)

(deftest expand-tests
  (testing "should work with a basic query"
    (let [g (basicGraph)
          result (expandWhere g ["ns:1" "ns:2"] {"data" "foo"})]
      (is (= result ["ns:1"]))))

  (testing "should work with a single key"
    (let [g (basicGraph)
          result (expandWhere g ["ns:1" "ns:2"] {"vertexKey" "ns:2"})]
      (is (= result ["ns:2"])))))


(deftest vertex-key-tests
  (testing "should work with 2 args"
    (is (= (vertex-key "foo" "123") "foo:123"))
    (is (= (vertex-key "foo" 123) "foo:123")))

  (testing "should work with a single array arg"
    (is (= (vertex-key ["foo" "123"]) "foo:123")))

  (testing "should work with a single string arg"
    (is (= (vertex-key "123") "V:123")))

  (testing "should work with an already valid key"
    (is (= (vertex-key "asdf:123") "asdf:123")))

  (testing "should work with a valid key in an array"
    (is (= (vertex-key ["asdf:123"]) "asdf:123"))))

(deftest mapset-tests
  (testing "should work with a vector"
    (let [result (mapset
                    (fn [item] (+ item 1))
                    (vec (range 0 5)))]
      (is (= result #{1 2 3 4 5}))))

  (testing "should work with a set"
    (let [result (mapset
                    (fn [item] (+ item 1))
                    #{0 1 2 3 4})]
      (is (= result #{1 2 3 4 5}))))

  (testing "should work with a map"
    (let [result (mapset
                    (fn [item] (+ (get item 0) 1))
                    {1 1, 2 2, 3 3})]
      (is (= result #{2 3 4}))))

  (testing "should work when it returns other vectors"
    (let [result (mapset
                    (fn [item] [item (- item 1)])
                    [1 2 3])]
      (is (= result #{0 1 2 3})))))

(deftest uniq-tests
  (testing "should work with vecs of numbers"
    (is (= (uniq [1 2 2 3]) [1 2 3])))

  (testing "should work with vecs of strings"
    (is (= (uniq ["1" "2" "2" "3"]) ["1" "2" "3"])))

  (testing "should work with sets of numbers"
    (is (= (uniq #{1 2 3}) [1 2 3])))

  (testing "should work with sets of strings"
    (is (= (uniq #{"1" "2" "3"}) ["1" "2" "3"])))
  )
