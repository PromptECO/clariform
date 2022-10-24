(ns clariform.core
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [instaparse.core :as insta
    :refer-macros [defparser]]
   [clariform.serialize :as serialize]))

(defn exit [status msg]
  (println msg)
  (.exit js/process status))

(defparser parse-strict
  (str (rc/inline "./strict.ebnf")
       (rc/inline "./tokens.ebnf")))

(defn parse-strict! [s]
  (let [ast (parse-strict s)]
    (if (insta/failure? ast)
      (exit 1 (pr-str ast))
      ast)))

(defn slurp [path]
  (let [fs (js/require "fs")]
    (.readFileSync fs path "utf8")))

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
      (exit 1 "Misformed parens"))))

#_
(defn format-indent [ast code]
  (-> (serialize/format-align ast)
      indent-code))

(defn format-compact [ast code]
  (serialize/format-compact ast))

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]
   [nil "--check" "Exit with error on invalid code, supressing output"]
   [nil "--format" "Format code"]])

(defn execute-command [{:keys [arguments options summary errors] :as opts}]
  (tap> arguments)
  (cond
    (not-empty errors)
    (exit 1 (clojure.string/join "\n" errors))
    (some? (:help options))
    (do
      (println "Painless linting and formatting for Clarity.")
      (println "Usage: clariform [options] file")
      (println "Options:")
      (println summary))
    (some? (:version options))
    (prn "0.0.3")
    (some? (:check options))
    (when (not-empty arguments)
      (doseq [item arguments]
        (->> item 
             (slurp)
             (parse-strict!))))
    (some? (:format options))
    (doseq [item arguments]
      (let [code (slurp item)]
        (when-let [ast (parse-strict! code)]
          (print (format-compact ast code)))))
    (empty? options)
    (doseq [item arguments]
      (let [code (slurp item)]
        (when-let [ast (parse-strict! code)]
          (print (indent-code code)))))
    :else 
    (do
      (prn (pr-str opts))
      (exit 1 "Invalid command"))))

(defn main [& args]
  (let [{:keys [arguments options summary errors] :as opts}
        (parse-opts args cli-options)]
    (execute-command opts)))

(defn ^:dev/after-load start []
  (main))
    
