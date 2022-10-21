(ns clariform.core-test
  (:require 
   [cljs.test :refer (deftest is)]
   [shadow.resource :as rc]
   [clariform.core :as clariform]))

(def basic-contract (rc/inline "./basic.clar"))  

(deftest parse-test
  (is (= (clariform/parse-strict basic-contract)
         [:S [:toplevel [:list [:symbol "define-read-only"] 
                         [:list [:symbol "inc"] [:list [:symbol "n"] [:symbol "int"]]] 
                         [:list [:symbol "+"] [:symbol "n"] [:int "1"]]]]])))
