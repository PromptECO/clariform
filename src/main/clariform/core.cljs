(ns clariform.core
  (:require 
   [clojure.tools.cli :refer [parse-opts]]
   [shadow.resource :as rc]
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
      (println "OK"))))

(defn slurp [path]
  (let [fs (js/require "fs")]
    (.readFileSync fs path "utf8")))

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]])

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
      (println "0.0.1")
      (not-empty arguments)
      (doseq [item arguments]
        (->> item 
             (slurp)
             (check-strict!)))
      :else 
      (do
        (prn (pr-str opts))
        (exit 1 "Invalid command")))))
