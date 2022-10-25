(ns clariform.ast.serialize
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [taoensso.timbre :as timbre]
   [clariform.token :as token]))  

(defn compact-spacing [mode front back]
  "Minimalistic spacing, each toplevel on its own line"
  (let [edge (or (nil? front) (nil? back))]
    (case mode 
      (:toplevel) 
      (if-not edge "\n")
      (:list :<>) 
      (if-not edge " ")
      (:prop) 
      (if-not edge ": ")
      (:record) 
      (if-not edge ", ")
      nil)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defmulti format-form token/tag)

(defn format-around [form options]
  (format-form form options))

(defn format [form options]
  (format-form form options))

(defn format-separated-form [spacing [front back] options]
  "Prefix a form with approproate separation"
  (str
    (spacing front back)
    (if (some? back)
      (format back options))))

(defn format-separated-items [mode forms & [{:keys [layout] :as options}]]
  {:post [string?]}
  ;; ensures required separators}
  (let [spacing (-> (case layout
                      ("compact") compact-spacing)
                    (partial mode))]
    (apply str 
      (mapcat 
       (partial format-separated-form spacing)
       (partition-all 2 1 (list* nil forms))
       (repeat options)))))

(defmethod format-form :default [form options]
   (pr-str form))

(defmethod format-form :int [form options]
   (apply str (token/content form)))

(defmethod format-form :uint [form options]
   ;; TODO: cover betweens, call method dispatch
   (apply str "u" (token/content form)))

(defmethod format-form :hex [form options]
   (apply str "0x" (token/content form)))
  
(defmethod format-form :list [form options]
  (str "(" 
    (format-separated-items 
     :list 
     (token/content form)
     options)
    ")")) 

(defmethod format-form :record [form options]
  (str "{" 
    (format-separated-items :record (token/content form) options)
   "}"))

(defmethod format-form :prop [form options] 
  (let [[k v & more] (token/content form)]
    (spec/assert empty? more)
    (format-separated-items :prop (token/content form) options)))

(defmethod format-form :string [form options]
  (let [escaped-string (token/form->str form)]
    (str (if (some #{[:UNICODE]} (token/content form)) "u")
         "\"" escaped-string "\"")))

(defmethod format-form :symbol [form options]
   (apply str (token/content form)))

(defmethod format-form :principal [form options]
   (token/form->str form))

(defmethod format-form :identifier [form options]
   (token/form->str form))

(defmethod format-form :toplevel [form options]
  (format-separated-items :toplevel (token/content form) options))

(defmethod format-form :S [form options]
  (format-separated-items :toplevel (token/content form) options))

(defn format-compact [ast]
  (format-form ast {:layout "compact"}))

