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
  (let [result (-> (.parenMode parinfer code #js{:cursorLine 0 :cursorX 0})
                   (js->clj :keywordize-keys true))]
    (when-some [error (:error result)]
      (throw (ex-info "Indent failed" (js->clj error) :parinfer)))
    (:text result)))

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

(defn remove-orphan-space [indented original & [{:keys [lint] :as options}]]
  "Collapse whitespace left from inferring indentation"
  (let [linted (if (some? indented)
                (->> (map string/split-lines [indented original])
                     (apply map vector)
                     (remove (fn [[indented-line code-line]]
                               (and (string/blank? indented-line)
                                    (not (string/blank? code-line)))))))
        collapsed (if (some? linted)
                    (loop [out []
                           source linted]
                      (if (nil? source)
                        out
                        (let [[[line1 original1] & more1] source]
                          (let [[[line2 original2] & more2] more1]
                            (cond
                              (and (or (get lint :hanging-open)
                                       (not= line2 original2))
                                   (#{"(" "{" "["} (last (string/trimr line1)))    
                                   (<= (count (string/trimr line1)) 
                                       (- (count line2) 
                                          (count (string/triml line2)))))
                              (recur 
                                (conj out (str line1 (subs line2 (count line1))))
                                more2)
                              :else
                              (recur 
                                (conj out line1)
                                more1)))))))]
    (->> collapsed
        (string/join "\n")
        (string/triml))))

(defn indent-code [code]
  (some-> code
          (infer-indent)
          (remove-orphan-space code {:lint true})))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;
;; FORMATTING

(defn format-retain [ast]
  "Retain original indentation"
  (serialize/format-retain ast))

(defn format-indent [ast]
  "Closing parenthesis never dangles nor is to the left of its matching opening parenthesis"
  (-> (format-retain ast)
      indent-code))

(defn format-align [ast]
  "Left-align by removing all indentation"
  (serialize/format-align ast))

(defn format-auto [ast]
  "Autoindent ignoring original indentation"
  (-> (format-align ast)
      indent-code))

(defn format-compact [ast]
  "Remove insignificant whitespace, placing each toplevel expression on a separate line"
  (serialize/format-compact ast))

(defn format-code [ast {:keys [format strict]}]
  (println "=" format)
  (case format
    ("retain" nil)       
    (format-retain ast)
    "indent"
    (format-indent ast)  
    "auto" 
    (format-auto ast)
    "align" 
    (format-align ast)
    "compact" 
    (format-compact ast)))
