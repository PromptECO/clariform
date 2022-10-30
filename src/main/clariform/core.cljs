(ns clariform.core
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

(defn exit [status msg]
  (println msg)
  (.exit js/process status))

(defn parse-strict! [s]
  (let [ast (parser/parse-strict s)]
    (if (insta/failure? ast)
      (exit 1 (pr-str ast))
      ast)))

(defn parse-code [code]
  (->> (parser/parse-strict code)
       (insta/add-line-and-column-info-to-metadata code)
       (#(add-between-to-metadata % code))))

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

(defn format-retain [ast code]
  (serialize/format-retain ast))

(defn format-align [ast code]
  (serialize/format-align ast))

(defn format-compact [ast code]
  (serialize/format-compact ast))

(defn format-file [path {:keys [format]}]
  (let [code (slurp path)]
    (when-let [ast (parse-code code)]
      (case format
        "indent" 
        (-> (format-align ast code)
            indent-code
            print)
        "align" 
        (-> (format-align ast code)
            print)
        "compact" 
        (-> (format-compact ast code)
            print)
        "retain" 
        (-> (format-retain ast code)
            print)))))

(defn check-file [path options]
  (let [code (slurp path) 
        ast (parser/parse-strict code)]
    (if (insta/failure? ast)
      ast)))

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]
   [nil "--check" "Exit with error on invalid code, supressing output"]
   [nil "--format FORMAT" "Output format"]
   [nil "--verbose"]])

(defn execute-command [{:keys [arguments options summary errors] :as opts}]
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
      (doseq [path arguments]
        (if-let [err (check-file path options)]
          (do
            (binding [*print-fn* *print-err-fn*]
              (println err))
            (exit 1))
          (if (:verbose options)
            (println path)))))
    (some? (:format options))
    (doseq [path arguments]
      (format-file path options))
    :else 
    (doseq [path arguments]
      (if (:verbose options)
        (println path))
      (let [code (slurp path)
            ast (parser/parse-strict code)]
        (if (insta/failure? ast)
          (binding [*print-fn* *print-err-fn*]
            (println ast))
          (print (indent-code code)))))))

(defonce command (atom nil))

(defn main [& args]
  (let [{:keys [arguments options summary errors] :as opts}
        (parse-opts args cli-options)]
    (execute-command opts)
    (reset! command opts)))

(defn ^:dev/before-load reload! []
  (println "# RELOADING SCRIPT"))

(defn ^:dev/after-load activate! []
  (println "# Executing command")
  (execute-command @command))

