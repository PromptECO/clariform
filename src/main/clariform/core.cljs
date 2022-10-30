(ns clariform.core
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [instaparse.core :as insta
    :refer-macros [defparser]]
   [clariform.ast.serialize :as serialize]
   [clariform.ast.parser :as parser]
   [clariform.format :as format
    :refer [format-code]]))

(defn exit [status msg]
  (println msg)
  (.exit js/process status))

(defn parse-strict! [s]
  (let [ast (parser/parse-strict s)]
    (if (insta/failure? ast)
      (exit 1 (pr-str ast))
      ast)))

(defn slurp [path]
  (let [fs (js/require "fs")]
    (.readFileSync fs path "utf8")))

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
   [nil "--strict" "Expect strict Clarity syntax"]
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
            (exit 1 (pr-str err)))
          (if (:verbose options)
            (println path)))))
    (some? (:format options))
    (doseq [path arguments]
      (-> (slurp path)
          (format-code options)
          (print)))
    :else 
    (doseq [path arguments]
      (if (:verbose options)
        (println path))
      (let [code (slurp path)
            ast (parser/parse-strict code)]
        (if (insta/failure? ast)
          (binding [*print-fn* *print-err-fn*]
            (println ast))
          (print (format/indent-code code)))))))

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

