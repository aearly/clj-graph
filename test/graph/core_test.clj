(ns graph.core-test
  (:require [clojure.test :refer :all]
            [graph.core :refer :all]))

(defn basicGraph []
  (let [g (create)
        v1 {"id" 1 "data" "foo"}
        v2 {"id" 2 "data" "bar"}
        v3 {"id" 3 "data" "baz"}]
    (-> g
      (addVertex "class" v1)
      (addVertex "class" v2)
      (addVertex "class" v3)
      (addEdge "rel" {"from" ["class" 1] "to" "class:2"})
      (addEdge "rel" {"from" ["class" 2] "to" "class:1"})
      (addEdge "otherrel" {"from" ["class" 2] "to" "class:2"}))))


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
                (addVertex "class" {})
                (addVertex "asdf" {})
                (addVertex "class" {}))
            result (getVertices g "class")]
        (is (= result {
          "class:0" {"id" "0"}
          "class:2" {"id" "2"}
        }))))
  )


  (testing "edges"
    (testing "should add a named edge"
      (let [g (basicGraph)]
        (is (= (get g "edges") {
          "rel" [["class:1" "class:2"] ["class:2" "class:1"]]
          "otherrel" [["class:2" "class:2"]]
          }))))

    (testing "should maintain an index of outgoing verts by edge type"
      (let [g (basicGraph)
            indexes (get g "indexes")]
        (is (= indexes {
          "rel" {
            "class:1" ["class:2"]
            "class:2" ["class:1"] }
          "otherrel" { "class:2" ["class:2"] }
        }))))

    (testing "should get a list of named edges"
      (let [g (basicGraph)
            result (getEdges g "rel")]
        (is (= result [
          ["class:1" "class:2"]
          ["class:2" "class:1"]
        ]))))

    (testing "should throw when connecting to a nonexistent node"
      (let [g (create)]
        (is (thrown-with-msg? AssertionError #"no such vertex"
          (addEdge g "rel" {"from" "class:1" "to" "class:2"})))))
  )


  (testing "relationships"
    (testing "should get verts connected by named outgoing edges"
      (let [g (basicGraph)]
        (is (= (getOutgoing g "rel" "class" "1") ["class:2"]))
        (is (= (getOutgoing g "rel" "class:2") ["class:1"]))))

    (testing "should get outgoing edges from an array of vertices"
      (let [g (basicGraph)]
        (is (=
          (getOutgoing g "rel" ["class:1" "class:2"])
          ["class:1" "class:2"]))))

    (testing "should get outgoing edges recursively"
      (let [g (-> (basicGraph)
                (addEdge "rel" {"from" "class:2" "to" "class:3"}))]
        (is (= (getOutgoingRecur g "rel" "class:1")
               ["class:1" "class:2" "class:3"]))))

    (testing "should get verts connected by all outgoing edges"
      (let [g (basicGraph)]
        (is (= (getAllOutgoing g "class:2") ["class:1" "class:2"]))))

    (testing "should get verts connected by named incoming edges"
      (let [g (basicGraph)]
        (is (= (getIncoming g "rel" "class" "1") ["class:2"]))
        (is (= (getIncoming g "rel" "class:2") ["class:1"]))))

    (testing "should get incoming edges from an array of vertices"
      (let [g (basicGraph)]
        (is (=
          (getIncoming g "rel" ["class:1" "class:2"])
          ["class:1" "class:2"]))))

    (testing "should get verts connected by all incoming edges"
      (let [g (basicGraph)]
        (is (= (getAllIncoming g "class:2") ["class:1" "class:2"]))))

    (testing "should expand edge results"
      (let [g (basicGraph)
            result (getAllOutgoing g "class:2")]
        (is (= (expand g result) [
          {"id" 1 "data" "foo"}
          {"id" 2 "data" "bar"}
        ]))))
  )
)



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
