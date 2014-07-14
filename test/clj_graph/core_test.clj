(ns clj-graph.core-test
  (:require [clojure.test :refer :all]
            [clj-graph.core :refer :all]))

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
