(ns clariform.core
  (:require 
   [cljs.core.async :as async
    :refer [go go-loop]]
   [cljs.core.async.interop :refer-macros [<p!]]
   [clojure.tools.cli :refer [parse-opts]]
   [cljs.pprint :as pprint
    :refer [pprint]]
   [clojure.string :as string]
   [shadow.resource :as rc]
   ["parinfer" :as parinfer]
   [instaparse.core :as insta
    :refer-macros [defparser]]
   [clariform.ast.serialize :as serialize]
   [clariform.ast.parser :as parser]
   [clariform.format :as format
    :refer [format-code]]
   [clariform.io :as io]
   [clariform.transform :as transform
    :refer [transform]]))

(def version-string "0.4.0")

(defn printerr [& vals]
  (binding [*print-fn* *print-err-fn*]
    (apply println vals)))

(defn exit [status msg]
  (printerr msg)
  (.exit js/process status))

(defn check-all [{:keys [arguments options summary errors] :as params}]
  (let [resources (io/resources-seq arguments)
        multiple (some? (next resources))
        con-chan (io/contracts-chan resources)]
    (go-loop []
      (binding [*out* (pprint/get-pretty-writer *out*)]
        (when-let [{:keys [locator error text] :as res} (<! con-chan)]
          (when multiple
            (pprint/fresh-line)
            (println (io/file-path locator)))
          (if (some? error)
            (if (:debug options)
              (printerr (ex-message error))
              (exit 1 (ex-message error)))
            (let [ast (parser/parse-strict text)]
              (when-let [err (if (insta/failure? ast)
                               (insta/get-failure ast))]
                (if (:debug options)
                  (printerr err)
                  (exit 1 (pr-str err))))))
          (recur))))))  
 
(defn format-all [{:keys [arguments options summary errors] :as opts}]
  (let [resources (io/resources-seq arguments)
        multiple (some? (next resources))
        con-chan (io/contracts-chan resources)]
    (go-loop [{:keys [locator error text] :as res} (<! con-chan)]
      (binding [*out* (pprint/get-pretty-writer *out*)]
        (when (some? res)
          (when error
            (printerr (ex-message error))
            (recur (<! con-chan)))
          (when multiple
            (pprint/fresh-line)
            (println ";;" (io/file-path locator)))
          (let [parser-options (select-keys options [:strict])
                ast (format/parse-code text parser-options)]
            (if (insta/failure? ast)
              (let [failure (insta/get-failure ast)]
                (printerr failure))
              (do
                (when-not (:strict options)
                  (doseq [node (transform/node-seq ast)]
                    (case (transform/node-tag node)
                      (:invalid :skip)
                      (printerr "Invalid or misplaced code on line" 
                                (apply str 
                                  (get-in (meta node) [:instaparse.gll/start-line]) ": " 
                                  (transform/node-content node)))
                      nil)))
                (try
                  (let [formatted (-> (transform ast)
                                      (format-code options))]
                    (print formatted))
                  (catch ExceptionInfo e
                    (printerr (ex-message e))))))
            (pprint/fresh-line)
            (when-some [more (<! con-chan)]
              (println)
              (recur more))))))))

(def cli-options
  [[nil "--version"]
   ["-h" "--help"]
   [nil "--check" "Exit with error on invalid code, supressing output"]
   [nil "--format FORMAT" "Output format"
    :default "indent"
    :validate [#{"retain" "adjust" "indent" "auto" "align" "compact"} 
               "Must be one of 'retain', 'adjust', 'indent', 'auto', 'align' or 'compact'"]]
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
    (println version-string)
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
  (println "\n-----"))

(defn ^:dev/after-load activate! []
  (println "Executing command:\n" 
           (select-keys @command [:options :arguments]) "\n>>>>>")
  (execute-command @command))

