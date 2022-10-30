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

(defn indent-code [code]
  (let [{:keys [text success]} 
        (-> (.parenMode parinfer code #js {:cursorLine 0 :cursorX 0})
            (js->clj :keywordize-keys true))]
    (if success
      (->> (map string/split-lines [text code])
           (apply map vector)
           (remove (fn [[indented-line code-line]]
                     (and (string/blank? indented-line)
                          (not (string/blank? code-line)))))
           (map first)
           (clojure.string/join "\n"))           
      (throw (ex-info "Misformed parens" {})))))

(defn format-retain [ast code]
  (serialize/format-retain ast))

(defn format-align [ast code]
  (serialize/format-align ast))

(defn format-compact [ast code]
  (serialize/format-compact ast))

(defn format-code [code {:keys [format strict]}]
  (when-let [ast (parser/parse-code code strict)]
    (case format
      "indent" 
      (-> (format-align ast code)
          indent-code)
      "align" 
      (-> (format-align ast code))
      "compact" 
      (-> (format-compact ast code))
      "retain" 
      (-> (format-retain ast code)))))