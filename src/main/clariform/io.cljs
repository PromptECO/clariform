(ns clariform.io
  (:require
    [cljs.core.async :as async
     :refer [go chan put! close!]]
    [cljs.core.async.interop 
     :refer-macros [<p!]]
    ["https" :as https]
    [cljs-node-io.core :as node-io
     :refer [slurp]]
    [cljs-node-io.file :as file
     :refer [File]]
    [cljs-node-io.async :as node-async]
    [cljs-node-io.url :as node-url]
    [goog.Uri :as uri]))

(defn file-path [file]
  (.getPath ^File file))

(defn file-ext [file]
  (.getExt ^File file))

(defn fetch-text [locator]
  (node-url/aslurp locator))

(defn contracts-seq [path]
  (let [url (uri/parse path)]
    (if (and url (#{"http" "https"} (.getScheme url)))
      (list url)
      (->> (node-io/file-seq path)
           (filter #(.isFile %))
           (filter #(or (= (file-path %) path)
                        (= (file-ext %) ".clar")))))))

(defn resources-seq [arguments]
  #_ ;; preferable
  (or (mapcat contracts-seq arguments) (contracts-seq "."))
  (if (empty? arguments) 
    (contracts-seq ".") 
    (mapcat contracts-seq arguments)))

(defn contracts-chan [resources]
  (let [resource-chan (async/chan)
        contract-chan (async/chan)
        fetch-code (fn [locator]
                     (async/map #(identity
                                  {:locator locator
                                   :text (if-not (instance? ExceptionInfo %1) %1)
                                   :error (if (instance? ExceptionInfo %1) %1)})
                                [(fetch-text locator)]))]                 
    (async/onto-chan! resource-chan resources)
    (async/pipeline-async 10 contract-chan
                          #(async/pipe (fetch-code %1) %2) 
                          resource-chan)
    contract-chan))
