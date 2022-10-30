(ns clariform.ast.parser
  (:require 
   [shadow.resource :as rc]
   [instaparse.core :as insta
    :refer-macros [defparser]]))

(def trailing-newline #(if (= \newline (last %)) % (str % \newline)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TOKENS

(def tokens-grammar (rc/inline "../../clarity/grammar/tokens.ebnf"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STRICT PARSER

(def strict-grammar (rc/inline "../../clarity/grammar/strict.ebnf"))

(defparser strict-parser
  (str strict-grammar tokens-grammar))

(defn parse-strict [code]
  (insta/parse strict-parser (trailing-newline code) :total true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ROBUST PARSER

;; Missing end brackets should always be a failure for compatibility with parinfer / paren-soup
;; Avoid using "ordered choice" in repetition as it lead to ambiguities (possible a bug in instaparse)

(def robust-grammar (rc/inline "../../clarity/grammar/robust.ebnf"))

(defparser robust-parser 
 (str
   robust-grammar
   tokens-grammar
   ; overrides
   "string = UNICODE? <DQUOTE> #'(?:[^\"\\\\]|\\\\.)*' <DQUOTE>\n"))

(defn parse-robust [code]
  (insta/parse robust-parser code :total true))

