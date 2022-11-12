(ns clariform.format
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [instaparse.core :as insta
    :refer-macros [defparser]]
   [clariform.ast.between :as between
    :refer [add-between-to-metadata]]
   [clariform.ast.serialize :as serialize]
   [clariform.ast.parser :as parser]))

(defn infer-indent [code]
  (-> (.parenMode parinfer code #js {:cursorLine 0 :cursorX 0})
      (js->clj :keywordize-keys true)
      :text))

(defn infer-parens [code]
  (-> (.indentMode parinfer code #js {:cursorLine 0 :cursorX 0})
      (js->clj :keywordize-keys true)
      :text))

(defn infer-normalize [code]
  (-> code infer-indent infer-parens))

(defn parse-code [code & [strict]]
  (let [parse (if strict parser/parse-strict parser/parse-robust)]
    (->> (parse code)
         (insta/add-line-and-column-info-to-metadata code)
         (#(add-between-to-metadata % code)))))

(defn indent-code [code]
  (let [text (infer-indent code)]
    (->> (map string/split-lines [text code])
         (apply map vector)
         (remove (fn [[indented-line code-line]]
                   (and (string/blank? indented-line)
                        (not (string/blank? code-line)))))
         (map first)
         (clojure.string/join "\n"))))

(defn format-retain [ast]
  (serialize/format-retain ast))

(defn format-align [ast]
  (serialize/format-align ast))

(defn format-compact [ast]
  (serialize/format-compact ast))

(defn format-indent [ast]
  (-> (format-align ast)
      indent-code))

(defn format-code [ast {:keys [format strict]}]
  (case format
    "indent" 
    (format-indent ast)
    "align" 
    (format-align ast)
    "compact" 
    (format-compact ast)
    ("retain" nil) 
    (format-retain ast)))
