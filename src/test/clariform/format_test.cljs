(ns clariform.format-test
  (:require 
   [cljs.test :refer (deftest is)]
   [clojure.string :as string]
   [shadow.resource :as rc]
   [clariform.core :as clariform]
   [clariform.ast.parser :as parser]
   [clariform.format :as format]))

(def basic-contract (rc/inline "../basic.clar")) 

(def malformed-contract (rc/inline "../malformed.clar"))

(deftest parse-test
  (is (= (parser/parse-strict basic-contract)
         [:S [:toplevel [:list [:symbol "define-read-only"] 
                         [:list [:symbol "inc"] [:list [:symbol "n"] [:symbol "int"]]] 
                         [:list [:symbol "+"] [:symbol "n"] [:int "1"]]]]])))

(deftest remove-orphans-test
  (is (= (format/remove-orphan-space "(a)\n\n(b)" "(a\n)\n(b)")
         "(a)\n(b)")))

(defn process-retain [code]
  (-> (format/parse-code code)
      (format/format-retain)))

(defn process-adjust [code]
  (-> (format/parse-code code)
      (format/format-adjust)))

(defn process-indent [code]
  (-> (format/parse-code code)
      (format/format-indent)))

(defn process-auto [code]
  (-> (format/parse-code code)
      (format/format-auto)))

(defn process-align [code]
  (-> (format/infer-indent code) ;; fixing dangling parens FIXME eliminate
      (format/parse-code)
      (format/format-align)))

(defn process-tight [code]
  (-> (format/parse-code code)
      (format/format-tight)))

(defn process-spread [code]
  (-> (format/parse-code code)
      (format/format-spread)))

(defn process-compact [code]
  (-> (format/parse-code code)
      (format/format-compact)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(deftest infer-parens-test
  (is (= (format/infer-parens "(hello")
         "(hello)"))
  (is (= (process-retain "(hello)")
         "(hello)"))
  #_ ;; should not be default
  (is (= (process-retain "(hello")
         "(hello)")
      "Append missing endparen")
  #_ ;; should not be default
  (is (= (process-retain "hello)")
         "hello")
      "Remove dangling endparen (is this preferable?)"))

(deftest infer-indent-test
  (is (= (format/infer-indent "(XXX\nYYY)")
         "(XXX\n YYY)"))
  (is (= (format/infer-indent "(XXX\n   YYY)")
         "(XXX\n   YYY)")
      "No change when the nesting is valid"))

(deftest format-retain-test
  (is (= (process-retain basic-contract)
         (string/trim basic-contract))))
  
(deftest format-adjust-test
  (is (= (process-adjust "(XXX\nYYY)")
         "(XXX\nYYY)"))
  (is (= (process-adjust "(list\n   (list\n)\n  )")
         "(list\n   (list\n   )\n)")
      "Line up close parens with the offset of the open parens"))

(deftest format-indent-test
  (is (= (process-indent "(XXX\nYYY)")
         "(XXX\n YYY)"))
  (is (= (process-indent "(XXX\n   YYY)")
         "(XXX\n   YYY)")
      "No change when the nesting is valid"))

(deftest format-auto-test
  (is (= (process-auto "(XXX\nYYY)")
         "(XXX\n  YYY)")
      "Offset children")
  (is (= (process-auto "(XXX\n   YYY)")
         "(XXX\n  YYY)")
      "Override original indentation"))

(deftest format-align-test
  (is (= (-> (format/parse-code basic-contract)
             (format/format-align))
         (str "(define-read-only (inc (n int))\n" 
              "(+ n 1))"))))

(deftest format-compact-test
  (is (= (-> (format/parse-code basic-contract)
             (format/format-compact))
         "(define-read-only (inc (n int)) (+ n 1))")))

#_
(deftest format-tight-test
  (is (= (-> (format/parse-code "(if (> n 0) n (- n))")
             (format/format-tight))
        (str 
         "(if (> n 0)\n" 
         "n\n"
         "(- n))"))))

(deftest format-spread-test
             (format/format-spread)
        (str 
         "(if\n"
         "(> n 0)\n" 
         "n\n"
         "(-\n"
         "n))"))

(deftest record-shorthand-test
  (is (= (process-retain "{a:1}")
         (process-retain "{a: 1}")
         (process-retain "{a:  1}")
         "{a: 1}")
      "Normalize property with a single space before value")
  (is (= (process-retain "{a}")
         "{a: a}")
      "Implicit property value is same as name")
  (is (= (process-retain "{a,b}")
         "{a: a, b: b}")
      "Complete multiple implicit property values")
  (is (= (process-retain "{a: 1,b}")
         "{a: 1, b: b}")
      "Mix implicit and explicit property values"))

(deftest malformed-correct-test 
  (is (= (format/parse-code malformed-contract)
         [:S [:toplevel [:list [:symbol "define-read-only"] 
                         [:list [:symbol "plus"] [:list [:symbol "n"] [:symbol "int"]]] 
                         [:list [:symbol "let"] [:list [:list [:symbol "value"] 
                                                              [:list [:symbol "+"] [:symbol "n"] [:int "1"]]]] 
                          [:symbol "value"]]]]])
      "Parse robustly even if the indent is messed up")
  (is (= (process-retain malformed-contract)
         malformed-contract)
      "Format as original but with end parens adjusted to line up")
  (is (= (process-adjust malformed-contract)
         (str ";; Original is valid with correctly balanced parens\n"
              ";; but having malformed layout and indentation.\n"
              "(define-read-only (plus\n"
              "                  (n int)\n"
              "                  )\n"
              "  (let (\n"
              "    (value (+ n 1))\n"
              "       )\n"
              "    value\n"
              "  )\n"
              ")"))
      "Format as original but with end parens adjusted to line up")
  (is (= (process-indent malformed-contract)
         (str ";; Original is valid with correctly balanced parens\n"
              ";; but having malformed layout and indentation.\n"
              "(define-read-only (plus\n"
              "                   (n int))\n"
              "  (let ((value (+ n 1)))\n"
              "    value))"))
      "Format with indentation (should indent two spaces and remove whitespace except comments after open parens)")
  (is (= (process-auto malformed-contract)
         (str ";; Original is valid with correctly balanced parens\n"
              ";; but having malformed layout and indentation.\n"
              "(define-read-only (plus\n"
              "                    (n int))\n"
              "  (let ((value (+ n 1)))\n"
              "    value))"))
      "Format with indentation (should indent two spaces and remove whitespace except comments after open parens)")
  (is (= (process-align malformed-contract)
         (str ";; Original is valid with correctly balanced parens\n"
              ";; but having malformed layout and indentation.\n"
              "(define-read-only (plus\n"
              "(n int))\n\n"
              "(let (\n"
              "(value (+ n 1)))\n\n"
              "value))"))
      "Format left-aligned (should also eliminate unnecessary newlines)")
  (is (= (process-compact malformed-contract)
         (str "(define-read-only (plus (n int)) "
              "(let ("
              "(value (+ n 1))) "
              "value))"))
      "Format with each toplevel form on a single line and minimized whitespace"))

(def codecov-contract (rc/inline "../sbtc-testnet-debug-controller.clar"))

(def spread-contract (rc/inline "../sbtc-testnet-debug-controller.spread.clar"))


(deftest codecov--test 
  (is (= (process-spread codecov-contract)
         spread-contract)
      "Codecov contract is formatted with spread expressions"))
