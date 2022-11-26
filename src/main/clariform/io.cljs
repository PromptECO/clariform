(ns clariform.io
  (:require
    [cljs-node-io.core :as node-io
     :refer [slurp]]
    [cljs-node-io.file :as file
     :refer [File]]))

(defn file-path [file]
  (.getPath ^File file))

(defn file-ext [file]
  (.getExt ^File file))

(defn contracts-seq [& [path]]
  (->> (node-io/file-seq (or path "."))
       (filter #(.isFile %))
       (filter #(or (= (file-path %) path)
                    (= (file-ext %) ".clar")))))



   
