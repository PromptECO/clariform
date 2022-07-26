(ns clariform.core-test
  (:require 
   [cljs.test :refer (deftest is)]
   [shadow.resource :as rc]
   [clariform.core :as clariform]))

(def basic-contract (rc/inline "./basic.clar"))  

(deftest parse-test
  (is (= (clariform/parse-strict basic-contract)
         [:S [:toplevel [:list [:symbol "+"] [:int "1"] [:int "2"]]]])))
