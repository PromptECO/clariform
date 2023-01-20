(ns clariform.parse-test
  (:require 
   [cljs.test 
    :refer [deftest is]]
   [instaparse.core :as insta]
   [taoensso.timbre :as timbre]
   [clariform.ast.parser :as parser
    :refer [robust-parser parse-robust
            strict-parser parse-strict]])) 

(defn parse-tester [parser] 
  (fn [code expected & [feedback]]
    (let [mode :default
          result (case mode
                        :allow-ambiguous
                        (insta/parse parser code :partial false :total true)
                        :default
                        (insta/parses parser code :partial false :total true))
          token (case mode allow-ambiguous result (first result))] 
      (when (insta/failure? result)
        (timbre/debug "insta/failure" 
                      (insta/get-failure result))) 
      (is (= token expected) feedback)
      (when (= mode :default)
        (is (= [token] result)  
            (str "Ambigous parsing... "
                 feedback))))))      

(def robust-test 
  (parse-tester robust-parser))

(def strict-test 
  (parse-tester strict-parser))

(def parse-test 
  (juxt robust-test strict-test)) 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ATOMS

(deftest parse-symbol 
  (robust-test "foo"
              [:S [:toplevel [:symbol "foo"]]]))

(deftest parse-integer 
  (robust-test "123456789"
              [:S [:toplevel [:int "123456789"]]])
  (robust-test "-123456789"
              [:S [:toplevel [:int "-123456789"]]]))

(deftest parse-uint 
  (robust-test "u42"
               [:S [:toplevel [:uint "42"]]]))

(deftest parse-hex 
  (robust-test "0xCF23E0"
               [:S [:toplevel [:hex "CF23E0"]]])
  (robust-test "0x00"
               [:S [:toplevel [:hex "00"]]])
  (robust-test "0x"
               [:S [:toplevel [:hex ""]]]
    "Empty byte buffer"))

(deftest parse-atoms 
  (robust-test "foo bar 123"
              [:S [:toplevel [:symbol "foo"]]
                  [:toplevel [:symbol "bar"]]
                  [:toplevel [:int "123"]]]))

(deftest parse-principal 
  (strict-test "'ST398K1WZTBVY6FE2YEHM6HP20VSNVSSPJTW0D53M\n"
               [:S [:toplevel [:principal "'" "ST398K1WZTBVY6FE2YEHM6HP20VSNVSSPJTW0D53M"]]]
               "Strict parsing of a simple principal")
  (let [code "'ST398K1WZTBVY6FE2YEHM6HP20VSNVSSPJTW0D53M"]
    (is (not= (insta/parses strict-parser (str code "\n") :partial false :total true)
              (insta/parses strict-parser code :partial false :total true))
        "Newline is (still) required for unambigous strict parsing of a simple principal"))  
  (robust-test "'ST398K1WZTBVY6FE2YEHM6HP20VSNVSSPJTW0D53M"
               [:S [:toplevel [:principal "'" "ST398K1WZTBVY6FE2YEHM6HP20VSNVSSPJTW0D53M"]]]
               "Robust parsing of a simple principal")
  (robust-test "'123456789ABCDEFGHIJKLMNOPQRS.hello 123"
               [:S 
                [:toplevel 
                 [:principal "'" "123456789ABCDEFGHIJKLMNOPQRS" 
                  [:identifier "." "hello"]]]
                [:toplevel 
                 [:int "123"]]])
  (robust-test "'S1G2081040G2081040G2081040G208105NK8PE5.snippet-0"
               [:S 
                [:toplevel 
                 [:principal "'" "S1G2081040G2081040G2081040G208105NK8PE5" 
                  [:identifier "." "snippet-0"]]]])
  (robust-test ".flip-coin-tax-office.tax-office-trait\n"
               [:S [:toplevel 
                    [:identifier "." "flip-coin-tax-office" 
                     [:field "." "tax-office-trait"]]]]
               "Identifiers can be used")
  (robust-test "'SZ2J6ZY48GV1EZ5V2V5RB9MP66SW86PYKKQ9H6DPR.a"
               [:S [:toplevel 
                    [:principal "'" "SZ2J6ZY48GV1EZ5V2V5RB9MP66SW86PYKKQ9H6DPR" 
                     [:identifier "." "a"]]]]
    "Contract name can be single character"))

(deftest parse-principal-incomplete
  (robust-test "'123456789ABCDEF.hello"
              [:S 
               [:toplevel 
                [:skip "'123456789ABCDEF.hello"]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STRING 

(deftest parse-string 
  (robust-test "\"ABC\""
              [:S [:toplevel [:string "ABC"]]])
  (robust-test "u\"unicode\""
              [:S [:toplevel [:string [:UNICODE] "unicode"]]]))
  
(deftest parse-string-escape-chars
  (robust-test "\"QUOTE\\\"UNQUOTE\""
              [:S [:toplevel [:string "QUOTE\\\"UNQUOTE"]]])
  (let [quote "\""
        backslash "\\"
        code (str quote backslash backslash quote)
        expected [:S [:toplevel [:string (str backslash backslash)]]]]
    (strict-test code expected
      "Clarity can escape a single backslash in a string")  
    (robust-test code expected
      "Parse string with a single escaped backslash (backslash-backslash) into same (decoding happens later)"))) 

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; WHITESPACE

(deftest parse-empty 
  (robust-test " " [:S]))

(deftest parse-leading-whitespace
  (robust-test "  foo" [:S [:toplevel [:symbol "foo"]]]))
  
(deftest parse-trailing-whitespace
  (robust-test "foo   " [:S [:toplevel [:symbol "foo"]]]))

(deftest parse-comment
  (robust-test "foo ;; comment " [:S [:toplevel [:symbol "foo"]]]))
          
;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; LIST

(deftest parse-simple-list 
  (robust-test "(a b c)"
              [:S [:toplevel [:list [:symbol "a"] [:symbol "b"] [:symbol "c"]]]]))

(deftest parse-empty-list 
  (robust-test "()"
              [:S [:toplevel [:list]]]
    "List can be empty, like in trait definition arglists")
  (robust-test "(  )"
              [:S [:toplevel [:list]]]
    "Allow whitespace in empty list, although usually stripped by parinfer"))

(deftest parse-nested-list 
  (robust-test "(let ((a 1)( b  2 )   (c 3)) (list a b c))"
              [:S 
                [:toplevel 
                  [:list 
                   [:symbol "let"] 
                   [:list 
                    [:list [:symbol "a"] [:int "1"]] 
                    [:list [:symbol "b"] [:int "2"]] 
                    [:list [:symbol "c"] [:int "3"]]] 
                   [:list [:symbol "list"] [:symbol "a"] [:symbol "b"] [:symbol "c"]]]]])) 

(deftest parse-fast-all-expressions-test
  (robust-test 
   "(define foo 3)
         ;; abcdef
    (list 1 2 3)
    ;; hello world
    foo"
   [:S 
    [:toplevel 
     [:list [:symbol "define"] [:symbol "foo"] [:int "3"]]] 
    [:toplevel 
     [:list [:symbol "list"] [:int "1"] [:int "2"] [:int "3"]]]
    [:toplevel [:symbol "foo"]]]))

(deftest parse-inline-comment
  (robust-test
   (str "(+ 1\n"
        "   2   ;; inline\n"
        "   3)")
   [:S 
    [:toplevel 
     [:list 
      [:symbol "+"] 
      [:int "1"] 
      [:int "2"] 
      [:int "3"]]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; RECORD

(deftest parse-simple-record
  (robust-test "{a: 1}" 
              [:S [:toplevel [:record [:prop [:symbol "a"] [:int "1"]]]]])
  (robust-test "{label 42, name \"Ryan\"}"
               [:S [:toplevel 
                    [:record 
                     [:prop [:symbol "label"] [:int "42"]]
                     [:prop [:symbol "name"] [:string "Ryan"]]]]])) 

(deftest parse-compact-record
  (robust-test "{a:1}" 
              [:S [:toplevel [:record [:prop [:symbol "a"] [:int "1"]]]]]))

(deftest parse-spacy-record
  (robust-test "{  a  :  1  }" 
              [:S [:toplevel [:record [:prop [:symbol "a"] [:int "1"]]]]]))

(deftest parse-multi-record 
  (robust-test "{a: 1, b: 2}"
         [:S 
          [:toplevel 
            [:record 
              [:prop [:symbol "a"] [:int "1"]]
              [:prop [:symbol "b"] [:int "2"]]]]]))

(deftest parse-compact-multi-record 
  (robust-test "{a:1,b:2}"
         [:S
          [:toplevel  
            [:record 
              [:prop [:symbol "a"] [:int "1"]]
              [:prop [:symbol "b"] [:int "2"]]]]]))

(deftest parse-empty-record 
  (robust-test "{}"
         [:S 
          [:toplevel 
           [:record 
             [:missing]]]]))

(deftest parse-record-value-missing 
  (robust-test "{a:}"
         [:S 
          [:toplevel 
           [:record 
             [:prop [:symbol "a"] [:missing]]]]]))

(deftest parse-record-pname-missing 
  (robust-test "{:1}"
         [:S 
          [:toplevel 
           [:record 
             [:prop [:missing] [:int "1"]]]]]))

(deftest parse-record-pvalue-only 
  (robust-test "{1}"
         [:S 
          [:toplevel 
           [:record 
             [:prop [:missing] [:int "1"]]]]]))

(deftest parse-record-shorthand
  (robust-test "{a,b}"
    [:S 
     [:toplevel 
      [:record 
        [:prop [:symbol "a"]] 
        [:prop [:symbol "b"]]]]]) 
  (robust-test "{a}\n"
    [:S 
     [:toplevel 
      [:record 
        [:prop [:symbol "a"]]]]])
  (robust-test "{r }"
    [:S 
     [:toplevel 
      [:record 
       [:prop [:symbol "r"]]]]]
    "Space after shorthand symbol is valid shorthand syntax (and avoids errors during editing)")
  (robust-test "{a,b,}"
    [:S 
     [:toplevel 
      [:record 
        [:prop [:symbol "a"]] 
        [:prop [:symbol "b"]]]]]
    "Comma allowed after last property"))
              
(deftest parse-incomplete-record
  (robust-test "{a:1"
              [:S 
               [:toplevel [:invalid "{"]]
               [:toplevel [:symbol "a"]]
               [:toplevel [:skip ":1"]]]
              "Missing record end")
  (robust-test "a:1"
               [:S 
                [:toplevel [:symbol "a"]] 
                [:toplevel [:skip ":1"]]]
               "Record property outside record delimiters")
  (robust-test ":2 "
               [:S [:toplevel [:skip ":2"]]]
               "Stand alone property delimiter"))

(deftest parse-extra-quotes 
   (robust-test "(+ 2 3) \")"
               [:S [:toplevel 
                    [:list [:symbol "+"] [:int "2"] [:int "3"]]] 
                [:toplevel 
                 [:invalid "\""]] 
                [:toplevel 
                 [:invalid ")"]]]
    "Apt to disable parens completion in paren-soup!"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TOPLEVEL INVALID SYNTAX

(deftest parse-invalid-toplevel
  (robust-test " @123"
               [:S [:toplevel [:skip "@123"]]]
    "Skip after invalid token (could be changed to only bypass the invalid token)")
  (robust-test " :"
               [:S [:toplevel [:skip ":"]]]
    "Skip misplaced token at end")
  (robust-test " :123"
               [:S [:toplevel [:skip ":123"]]]
    "Skip after a misplaced colon (could be changed to only bypass the invalid colon)")
  (robust-test ",456 789"
               [:S [:toplevel [:skip ",456"]] [:toplevel [:int "789"]]]
    "Skip after a misplaced comma (could be changed to only bypass the invalid comma, and avoiding a new toplevel)"))

(deftest comment-not-barfing
  (robust-test ";; ok\n"
               [:S]
     "Comments are ignored")
  (robust-test "; missing semicolon\n123"
             [:S [:toplevel [:int "123"]]]
             "Don't make a fuss about a single semicolon")
  (robust-test ";\n42\n"
               [:S [:toplevel [:int "42"]]]
               "Single semicolon with no comment on the line")
  (robust-test "; missing semicolon\n"
             [:S]
             "Single semicolon at the end of code")
  (robust-test "; eof"
             [:S]
             "Comment doesn't require newline at end")
  (robust-test ";"
             [:S]
             "Nothing but an empty comment"))

(deftest parse-list-invalid-before
  (robust-test "@ (a b c)"
              [:S [:toplevel [:skip "@"]]
                  [:toplevel [:list [:symbol "a"] [:symbol "b"] [:symbol "c"]]]]))
                  
(deftest parse-list-invalid-after
  (robust-test "(a b c) @"
              [:S [:toplevel [:list [:symbol "a"] [:symbol "b"] [:symbol "c"]]]
                  [:toplevel [:skip "@"]]]))

(deftest parse-list-illegal-content 
  (robust-test "(@)"
              [:S [:toplevel [:list [:skip "@"]]]]))

(deftest parse-list-extra-close
  (robust-test "(a b c))"
              [:S [:toplevel [:list [:symbol "a"] [:symbol "b"] [:symbol "c"]]]
                  [:toplevel [:invalid ")"]]]))

(deftest parse-list-not-close 
  (robust-test "(a b c "
              [:S 
               [:toplevel [:invalid "("]]
               [:toplevel [:symbol "a"]]
               [:toplevel [:symbol "b"]] 
               [:toplevel [:symbol "c"]]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STRICT 

(deftest strict-record-miss-comma
  (strict-test "(list 1 2 3)\n\n{a: 1 ;; no comma\n b: 2}\n\n(list 1 2 3)"
               [:S [:instaparse/failure "(list 1 2 3)\n\n{a: 1 ;; no comma\n b: 2}\n\n(list 1 2 3)"]]
     "error encapsulating record"))
