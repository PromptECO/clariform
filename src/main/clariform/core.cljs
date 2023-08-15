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
   [cljs-node-io.core :as node-io]
   [cljs-node-io.file :as node-file]
   [clariform.io :as io]
   [clariform.transform :as transform
    :refer [transform]]))

(def version-string "0.6.0")

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
 
(defn format-all [{:keys [arguments options summary errors] :as opts} & {:keys [output-dir]}]
  (let [resources (doall (io/resources-seq arguments))
        multiple (some? (next resources))
        con-chan (io/contracts-chan resources)]
    (go-loop [{:keys [locator error text] :as res} (<! con-chan)]
      (when (some? res)
        (when (some? error)
          (printerr (ex-message error))
          (recur (<! con-chan)))
        (when multiple
          (println "") ;; pprint/fresh-line fails!
          (when (some? locator)
            (println ";; " (.toString locator))))
        (binding [*out* (pprint/get-pretty-writer *out*)]
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
                        ;_ (node-io/spit "contracts-out/file61-locator.clar" (pr-str locator))
                        ;dir (if true output-dir #_(node-io/as-file output-dir))
                        ;_ (node-io/spit "contracts-out/file62-dir.clar" (pr-str dir))
                        ;filepath (case :prod
                        ;           :prod (node-io/filepath (str output-dir) #_"contracts-out" (str locator) #_"contracts/basic.clar")
                        ;           :test "contracts-out/contracts/basic.clar")]
                    ;(node-io/spit "contracts-out/file70-filepath.clar" (pr-str filepath))
                    ; (apply node-io/make-parents (string/split filepath #"/"))
                    (if-some [path (and (some? output-dir)
                                        (node-io/filepath (str output-dir) (str locator)))]
                      (do #_(node-io/spit "contracts-out/filexx.clar" (pr-str path))
                          ;; FIXME should work fine with urls rather than create "http" directory:
                          (apply node-io/make-parents (string/split path #"/"))              
                          (node-io/spit path formatted))
                      (print formatted)))
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
   [nil "--format FORMAT" "Output format for code"
    :default "indent"
    :validate [#{"retain" "adjust" "indent" "auto" "align" "tight" "spread" "compact"} 
               "Must be one of 'retain', 'adjust', 'indent', 'auto', 'align', 'tight', 'spread' or 'compact'"]]
   [nil "--strict" "Expect strict Clarity syntax"]
   ["-o" "--output-dir DIR" "Output directory"]
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
    (format-all params :output-dir (:output-dir options))))
    
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

