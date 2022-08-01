(ns clariform.core
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [instaparse.core :as insta
    :refer-macros [defparser]]))

(defn exit [status msg]
  (println msg)
  (.exit js/process status))

(defparser parse-strict
  (str (rc/inline "./strict.ebnf")
       (rc/inline "./tokens.ebnf")))

(defn check-strict! [s]
  (let [ast (parse-strict s)]
    (if (insta/failure? ast)
      (exit 1 (pr-str ast))
      ast)))

(defn slurp [path]
  (let [fs (js/require "fs")]
    (.readFileSync fs path "utf8")))

(defn structural-indent [code]
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

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]
   [nil "--check" "Exit with error on invalid code, supressing output"]])

(defn main [& args]
  (let [{:keys [arguments options summary errors] :as opts}
        (parse-opts args cli-options)]
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
      "0.0.2"
      (some? (:check options))
      (when (not-empty arguments)
        (doseq [item arguments]
          (->> item 
               (slurp)
               (check-strict!))))
      (not-empty arguments)
      (doseq [item arguments]
        (let [code (slurp item)]
          (when (check-strict! code)
            (print (structural-indent code)))))
      :else 
      (do
        (prn (pr-str opts))
        (exit 1 "Invalid command")))))
