(ns clariform.parse-test
  (:require 
   [cljs.test :refer (deftest is)]
   [clojure.string :as string]
   [shadow.resource :as rc]
   [clariform.core :as clariform]
   [clariform.ast.parser :as parser]
   [clariform.format :as format]))

(def basic-contract (rc/inline "./basic.clar"))  

(deftest parse-test
  (is (= (parser/parse-strict basic-contract)
         [:S [:toplevel [:list [:symbol "define-read-only"] 
                         [:list [:symbol "inc"] [:list [:symbol "n"] [:symbol "int"]]] 
                         [:list [:symbol "+"] [:symbol "n"] [:int "1"]]]]])))

(deftest format-retain-test
  (is (= (-> (parser/parse-code basic-contract)
             (format/format-retain basic-contract))
         (string/trim basic-contract))))

(deftest format-align-test
  (is (= (-> (parser/parse-code basic-contract)
             (format/format-align basic-contract))
         (str "(define-read-only (inc (n int))\n" 
              "(+ n 1))"))))

(deftest format-compact-test
  (is (= (-> (parser/parse-code basic-contract)
             (format/format-compact basic-contract))
         "(define-read-only (inc (n int)) (+ n 1))")))

