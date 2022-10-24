(ns clariform.token
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]))

(defn tag [form]
  (first form))

(defn content [form]
  (rest form))

(defn form? [inst]
  (keyword? (tag inst)))

(spec/fdef form->str 
  :args (spec/cat :form form?)
  :ret string?)

(defn form->str [form]
  (if (form? form)
    (apply str 
      (map #(cond 
              (string? %) %
              (form? %) (form->str %)         
              :else (pr-str %))
           (content form)))
    ""))
