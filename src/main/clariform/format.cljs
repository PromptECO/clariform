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
  (-> (.indentMode parinfer code #js {:cursorLine 0 :cursorX 0})
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
    (->> (parse (if strict code (infer-parens code)))
         (insta/add-line-and-column-info-to-metadata code)
         (#(add-between-to-metadata % code)))))

(defn indent-code [code]
  (let [text (infer-parens code)]
    (->> (map string/split-lines [text code])
         (apply map vector)
         (remove (fn [[indented-line code-line]]
                   (and (string/blank? indented-line)
                        (not (string/blank? code-line)))))
         (map first)
         (clojure.string/join "\n"))))

(defn format-retain [ast code]
  (serialize/format-retain ast))

(defn format-align [ast code]
  (serialize/format-align ast))

(defn format-compact [ast code]
  (serialize/format-compact ast))

(defn format-code [code {:keys [format strict]}]
  (when-let [ast (parse-code code strict)]
    (case format
      "indent" 
      (-> (format-align ast code)
          indent-code)
      "align" 
      (format-align ast code)
      "compact" 
      (format-compact ast code)
      ("retain" nil) 
      (format-retain ast code))))
