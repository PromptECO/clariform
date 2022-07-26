(ns clariform.core
  (:require 
   [shadow.resource :as rc]
   [instaparse.core :as insta
    :refer-macros [defparser]]))

(defparser parse-strict
  (str (rc/inline "./strict.ebnf")
       (rc/inline "./tokens.ebnf")))

(defn main [& cli-args]
  (prn "done"))
