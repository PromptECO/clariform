(ns clariform.format-test
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

(defn process-retain [code]
  (-> (format/parse-code code)
      (format/format-retain code)))

(deftest infer-parens-test
  (is (= (format/infer-parens "(hello")
         "(hello)"))
  (is (= (process-retain "(hello)")
         "(hello)"))
  (is (= (process-retain "(hello")
         "(hello)")
      "Append missing endparen")
  (is (= (process-retain "hello)")
         "hello")
      "Remove dangling endparen (is this preferable?)"))

(deftest format-retain-test
  (is (= (process-retain basic-contract)
         (string/trim basic-contract))))

(deftest format-align-test
  (is (= (-> (format/parse-code basic-contract)
             (format/format-align basic-contract))
         (str "(define-read-only (inc (n int))\n" 
              "(+ n 1))"))))

(deftest format-compact-test
  (is (= (-> (format/parse-code basic-contract)
             (format/format-compact basic-contract))
         "(define-read-only (inc (n int)) (+ n 1))")))
