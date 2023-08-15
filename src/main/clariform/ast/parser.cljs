(ns clariform.ast.parser
  (:require 
   [shadow.resource :as rc]
   [instaparse.core :as insta
    :refer-macros [defparser]]))

(def trailing-newline #(if (= \newline (last %)) % (str % \newline)))

(defn parse [parser code & args]
  (apply insta/parse parser (trailing-newline code) args))

(defn parses [parser code & args]
  (apply insta/parses parser (trailing-newline code) args))

(defn add-line-and-column-info-to-metadata [parsed code]
  (insta/add-line-and-column-info-to-metadata (trailing-newline code)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; TOKENS

(def tokens-grammar (rc/inline "../../clarity/grammar/tokens.ebnf"))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; STRICT PARSER

(def strict-grammar (rc/inline "../../clarity/grammar/strict.ebnf"))

(defparser strict-parser
  (str strict-grammar tokens-grammar))

(defn parse-strict [code]
  (parse strict-parser code :total true))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; ROBUST PARSER

;; Missing end brackets should always be a failure for compatibility with parinfer / paren-soup
;; For the grammar, avoid using "ordered choice" in repetition 
;; as it leads to ambiguities (possible a bug in instaparse)

(def robust-grammar (rc/inline "../../clarity/grammar/robust.ebnf"))

(defparser robust-parser 
 (str
   robust-grammar
   tokens-grammar
   ; overrides
   "string = UNICODE? <DQUOTE> #'(?:[^\"\\\\]|\\\\.)*' <DQUOTE>\n"))

(defn parse-robust [code]
  (parse robust-parser code :total true))

