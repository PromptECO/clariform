(ns clariform.core
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [cljs.pprint :as pprint
    :refer [pprint]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [cljs-node-io.core
    :refer [slurp]]
   [cljs-node-io.file :as file
    :refer [File]]
   [instaparse.core :as insta
    :refer-macros [defparser]]
   [clariform.ast.serialize :as serialize]
   [clariform.ast.parser :as parser]
   [clariform.format :as format
    :refer [format-code]]
   [clariform.io :as io]))

(def version-string "0.0.9")

(defn printerr [e]
  (binding [*print-fn* *print-err-fn*]
    (println e)))

(defn exit [status msg]
  (printerr msg)
  (.exit js/process status))

(defn parse-strict! [s]
  (let [ast (parser/parse-strict s)]
    (if (insta/failure? ast)
      (exit 1 (pr-str ast))
      ast)))

(defn check-file [path options]
  (let [code (slurp path) 
        ast (parser/parse-strict code)]
    (if (insta/failure? ast)
      (insta/get-failure ast))))

(defn check-all [{:keys [arguments options summary errors] :as params}]
  (let [contracts (if (empty? arguments) (io/contracts-seq) (mapcat io/contracts-seq arguments))]
    (doseq [path (if (empty? arguments) (io/contracts-seq) (mapcat io/contracts-seq arguments))]
      (when (:debug options)
        (println "CHECKING:" (io/file-path path)))
      (when-let [err (check-file (io/file-path path) options)]
        (if (:debug options)
          (printerr err)
          (exit 1 (pr-str err)))))))

(defn format-all [{:keys [arguments options summary errors] :as opts}]
  (let [contracts (if (empty? arguments) (io/contracts-seq) (mapcat io/contracts-seq arguments))]
    (println "ARGUMENTS:" arguments)
    (println "RESOLVE:" (map io/contracts-seq arguments))
    (println "CONTRACTS:" contracts)
    (doseq [[path & [more]] (partition-all 2 1 contracts)]
      (when (:debug options)
        (println "FORMAT: " (io/file-path path)))
      (when (next contracts)
        (pprint/fresh-line)
        (println ";;" (io/file-path path)))
      (let [code (slurp path)
            ast (format/parse-code code (:strict options))]
        (if (insta/failure? ast)
          (let [failure (insta/get-failure ast)]
            (printerr failure))
          (try
            (print (format-code ast options))
            (catch ExceptionInfo e
              (printerr (ex-message e))
              (printerr (ex-data e))))))
      (when (some? more)
        (pprint/fresh-line)
        (println)))))

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]
   [nil "--check" "Exit with error on invalid code, supressing output"]
   [nil "--format FORMAT" "Output format"
    :default "indent"
    :validate [#{"indent" "align" "compact" "retain"} 
               "Must be one of 'indent', 'retain', 'align' or 'compact'"]]
   [nil "--strict" "Expect strict Clarity syntax"]
   [nil "--verbose"]
   [nil "--debug"]])

(defn execute-command [{:keys [arguments options summary errors] :as params}]
  (when (:debug options)
    (println "EXECUTE:")
    (pprint params))
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
    (prn version-string)
    (some? (:check options))
    (check-all params)
    :else
    (format-all params)))
    
(defonce command (atom nil))

(defn main [& args]
  (let [{:keys [arguments options summary errors] :as opts}
        (parse-opts args cli-options)]
    (binding [*out* (pprint/get-pretty-writer *out*)]
      (execute-command opts))
    (reset! command opts)))

(defn ^:dev/before-load reload! []
  #_(println "# RELOADING SCRIPT" @command))

(defn ^:dev/after-load activate! []
  (binding [*out* (pprint/get-pretty-writer *out*)]
    (println "\n-----\nExecuting command:\n" @command "\n>>>>>")
    (execute-command @command)))
