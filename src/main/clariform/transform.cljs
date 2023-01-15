(ns clariform.transform
  (:require
   [clojure.spec.alpha :as spec]
   [clariform.ast.between :as between]))

(defn node-tag [node]
  (if (vector? node)
    (first node)))

(defn node-content [node]
  (rest node))

(defn node-head [node]
   (first (node-content node)))

(defn update-content [node f]
  (between/with-meta-from node
    (into (subvec node 0 1) 
          (f (map identity (subvec node 1))))))

(defn update-children [node f]
  (between/with-meta-from node
    (into (subvec node 0 1) 
          (map f (subvec node 1)))))

(defn list-symbol [node]
  (if (and (= (node-tag node) :list)
           (= (node-tag (first (node-content node))) :symbol))
    (node-head (node-head node))))

(defn node-seq [node]
  "Traverse node into a sequence of the node and its children"
  (tree-seq vector? (comp (partial filter vector?) node-content) node))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn toplevel? [node]
  (= (node-tag node) :toplevel))

(spec/def ::toplevel-definition 
  (spec/and))

(spec/def ::toplevel (spec/and toplevel?
                               (spec/or :function-definition 
                                        #(#{"define-read-only" 
                                            "define-public"
                                            "define-private"} 
                                           (list-symbol (node-head %)))
                                        :toplevel-expression any?)))                               

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn wrap-begin [content]
  (between/update-spacing
    (into [:list [:symbol "begin"]] content)
    (fn [b]
      (assoc b
            :before (between/separator "\n")
            :after (between/separator "\n")))))

(defn transform-function-definition [node]
  (let [content (node-content node)]
    (if (<= (count content) 3)
      node
      (update-content node (fn [content]
                             (concat 
                              (take 2 content)
                              (list (wrap-begin (drop 2 content)))))))))

(defn transform-toplevel [node]
  {:pre [(toplevel? node)]}
  (case (first (spec/conform ::toplevel node))
    :function-definition
    (update-children node transform-function-definition)
    node))

(defn transform [ast]
  (update-children ast transform-toplevel))
