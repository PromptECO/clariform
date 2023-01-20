(ns clariform.serialize-test
  (:require 
   [cljs.test 
    :refer [deftest is]]
   [clariform.token :as token]
   [clariform.ast.between :as between]
   [clariform.ast.serialize :as serialize]))

(defn emit [form options]
  (serialize/format-form form options)) 

(deftest basic-serialization 
  (let [basic-form [:list [:symbol "foo"] [:int "1"]]]
    (is (= (token/tag basic-form) :list))
    (is (= (serialize/format-form basic-form {})
           "(foo 1)"))))
         
(deftest serialize-basic-record 
  (let [form [:record [:prop [:symbol "a"] [:int "1"]]
                      [:prop [:symbol "b"] [:int "2"]]]]
    (is (= (emit form {:layout "compact"})
           "{a: 1, b: 2}"))
    (is (= (emit form {:layout "retain"})
           "{a: 1, b: 2}"))))
