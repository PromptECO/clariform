(ns clariform.io
  (:require
    [cljs.core.async :as async
     :refer [go]]
    [cljs.core.async.interop 
     :refer-macros [<p!]]
    ["node-fetch" :as fetch]
    [cljs-node-io.core :as node-io
     :refer [slurp]]
    [cljs-node-io.file :as file
     :refer [File]]
    [goog.Uri :as uri]))

(defn file-path [file]
  (.getPath ^File file))

(defn file-ext [file]
  (.getExt ^File file))

(defn contracts-seq [path]
  (let [url (uri/parse path)]
    (if (and url (#{"http" "https"} (.getScheme url)))
      (list url)
      (->> (node-io/file-seq path)
           (filter #(.isFile %))
           (filter #(or (= (file-path %) path)
                        (= (file-ext %) ".clar")))))))

(defn fetch-text [resource]
  (if (cljs.core/instance? goog.Uri resource)
    (go (try 
          (let [response (<p! (fetch resource))]
            (if (.-ok response)
              (<p! (.text response))
              (ex-info "HTTP error" {:error response} ::io)))
          (catch :default e 
            e)))
    (go (slurp resource))))

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
