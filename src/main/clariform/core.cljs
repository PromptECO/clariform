(ns clariform.core
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [cljs-node-io.core :as io 
    :refer [slurp]]
   [cljs-node-io.file :as file
    :refer [File]]
   [instaparse.core :as insta
    :refer-macros [defparser]]
   [clariform.ast.serialize :as serialize]
   [clariform.ast.parser :as parser]
   [clariform.format :as format
    :refer [format-code]]))

(defn file-path [file]
  (.getPath ^File file))

(defn file-ext [file]
  (.getExt ^File file))

(defn printerr [e]
  (binding [*print-fn* *print-err-fn*]
    (println e)))

(defn exit [status msg]
  (printerr msg)
  (.exit js/process status))

(defn contracts-seq [& [path]]
  (->> (io/file-seq (or path "."))
       (filter #(.isFile %))
       (filter #(or (= (file-path %) path)
                    (= (file-ext %) ".clar")))))

(defn parse-strict! [s]
  (let [ast (parser/parse-strict s)]
    (if (insta/failure? ast)
      (exit 1 (pr-str ast))
      ast)))

(defn check-file [path options]
  (let [code (slurp path) 
        ast (parser/parse-strict code)]
    (if (insta/failure? ast)
      ast)))

(defn check-all [{:keys [arguments options summary errors] :as params}]
  (when (not-empty arguments)
    (doseq [path arguments]
      (if-let [err (check-file path options)]
        (do
          (binding [*print-fn* *print-err-fn*]
            (println err))
          (exit 1 (pr-str err)))
        (if (:verbose options)
          (println path))))))

(defn format-all [{:keys [arguments options summary errors] :as opts}]
  (let [files (if (empty? arguments) (contracts-seq) (mapcat contracts-seq arguments))
        options (update options :format (fnil identity "indent"))]
    (when (:debug options)
      (println "DEBUG:" options))
    (doseq [path files]
      (if (or (:verbose options)
              (some? (next files)))
        (println (file-path path)))
      (let [code (slurp path)
            code (if (:strict options) code (format/infer-normalize code))
            ast (format/parse-code code (:strict options))]
        (if (insta/failure? ast)
          (let [failure (insta/get-failure ast)]
            (printerr failure))
          (try
            (print (format-code ast options))
            (catch ExceptionInfo e
              (printerr (ex-message e))
              (printerr (ex-data e)))))))))

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]
   [nil "--check" "Exit with error on invalid code, supressing output"]
   [nil "--format FORMAT" "Output format"]
   [nil "--strict" "Expect strict Clarity syntax"]
   [nil "--verbose"]
   [nil "--debug"]])

(defn execute-command [{:keys [arguments options summary errors] :as params}]
  (when (:debug options)
    (println "EXECUTE:" params))
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
    (prn "0.0.4")
    (some? (:check options))
    (check-all params)
    :else
    (format-all params)))
    
(defonce command (atom nil))

(defn main [& args]
  (let [{:keys [arguments options summary errors] :as opts}
        (parse-opts args cli-options)]
    (execute-command opts)
    (reset! command opts)))

(defn ^:dev/before-load reload! []
  (println "# RELOADING SCRIPT" @command))

(defn ^:dev/after-load activate! []
  (println "# Executing command:" @command)
  (execute-command @command))
