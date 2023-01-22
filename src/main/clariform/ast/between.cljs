(ns clariform.ast.between
  "Capture separation between forms as metadata for optional restoration of original comments and formatting"
  (:require 
   [clojure.string :as str]
   [clojure.spec.alpha :as spec]
   [taoensso.timbre :as timbre]
   [instaparse.core :as insta]))

(defrecord ToplevelSeparator [sep edge])

(defn toplevel-separator [sep & [edge]]
  {:pre [(string? sep)]}
  (->ToplevelSeparator sep edge))

(defrecord RawOutput [s])

(defn raw-output [s]
  {:pre [(string? s)]}
  (->RawOutput s))

(defrecord Separator [text])

(defn separator [& [text]]
  (cond 
    (or (string? text) (nil? text))
    (->Separator text)
    :else text))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn with-meta-from [original generated]
  "Use meta from original as defaults for the meta of generated"
  (if (and (satisfies? IMeta original)
           (satisfies? IMeta generated))
    (if (insta/failure? (meta original))
      (vary-meta generated (partial merge (meta original)))
      (vary-meta generated merge (meta original)))
    generated))

(defn update-spacing 
  ([form f]
   (if (satisfies? IMeta form)
     (vary-meta form 
                update-in [:between] f)
     form))
  ([form where f]
   (if (satisfies? IMeta form)
     (vary-meta form 
                update-in [:between where] f)                
     form)))

(defn with-default-spacing [form]
  (if (satisfies? IMeta form) 
    (vary-meta form dissoc :between)
    form))

(defn get-spacing [form & where]
  (if (satisfies? IMeta form)
    (if-let [between (some-> (meta form)
                             (get :between))]
      (if (some? where)
        (get-in between (vec where) nil)
        between))))

(defn remove-blank-lines [content]
  (->> content
       (apply str)
       (str/split-lines)
       (remove str/blank?)
       (map str/trimr)
       (clojure.string/join "\n")))

(defn default-separation-fn [code] 
  (fn [start end & [edge]]
    {:post [(or (string? %) (nil? %))]}
    (if-not (or (not start) (not end) (= start end))
      (subs code start end))))

(defn s-separation-fn [code start-index end-index]
  (fn [start end]
    (let [opening (= start start-index)
          closing (= end end-index)
          edge (or opening closing)]
      (toplevel-separator
         (subs code start end)
         edge))))

(defn list-item-separator-fn [token code]
  (let [[start-index end-index] (insta/span token)]
    (fn [start end & [edge]]
      ;; only keep separator if not already the default, making it easier to override in gebnerated code
      (let [opening (and start-index (= start start-index))
            closing (and end-index (= end end-index))
            sep (subs code (if opening (min (inc start) end) start)
                           (if closing (max start (dec end)) end))]
        (if-not (or (and opening (= sep ""))
                    (and closing (= sep ""))
                    (and (not opening) (not closing) (= sep " ")))
          sep)))))

(defn clean-toplevel-preamble [preamble]
  (let [gap (-> preamble (remove-blank-lines))]
    (if-not (empty? gap)       
      (str "\n\n" gap))))

(defn mapcat-token-content [func token code & [{:keys [separation] :as options}]]
  {:post [vector?]}
  ;; Map function over the content of the token including unprocessed text between items and edges
  (let [[start-index end-index] (insta/span token)
        separation (or separation (default-separation-fn code))] 
    #_
    (assert (and start-index end-index)
            (pr-str "No parsed index for:" token start-index end-index))
    (if-let [children (rest token)]
      (loop [n 0
             ix start-index 
             children children
             result []]
        (if (empty? children)
          result
          (let [[item] children
                [item-start item-end] (insta/span item)
                edge (= n 0)]
            (recur
              (inc n) 
              (if (string? item)
                (+ ix (count item)) 
                item-end)
              (rest children)
              (apply conj result
                (if (string? item) 
                  (do  (timbre/error "Unreachable:" item) 
                    (func item nil nil))  ;; unreachable? if so, remove 
                  (let [before (separation ix item-start edge)
                        after (if (empty? (next children))
                                (separation item-end end-index true))]
                    (func before item after))))))))
      ;; TODO: shouldn't happen?
      [(raw-output (subs code start-index end-index))])))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn map-add-between [func token code & [separation-fn]]
  {:post [vector?]}
  (mapcat-token-content
   (fn [preamble item & [after]]
     (let [content (if item (func item))
           preamble (if (and (= (first item) :toplevel)
                             (empty? content))
                      (clean-toplevel-preamble preamble)
                      preamble)]
      (if (and true (some? content))
        (vector
           (vary-meta content assoc 
                      :between {:before (if (some? preamble) 
                                          (separator preamble))
                                :after (if (some? after) 
                                         (separator after))})) 
        ; can be collapsed - only when content is empty so see toplevel above
        (if (and (nil? content) (or (string? preamble) (string? after)))
          [(separator (str preamble after))]))))
   token code {:separation separation-fn}))

(defmulti add-betweens first)

(defn add-betweens-default [token code & [separation]]
  (if (some? (next token))
    (with-meta-from token
      (apply vector (first token) 
        (map-add-between #(add-betweens % code) token code separation)))
    token))

(defmethod add-betweens :default [token code]
  token)

(defmethod add-betweens :S [token code]
  (let [[start-index end-index] (insta/span token)
        separation (s-separation-fn code start-index end-index)]
    (add-betweens-default token code separation)))

(defmethod add-betweens :toplevel [token code]
   (add-betweens-default token code))

(defmethod add-betweens :list [token code]
  (let [separation (list-item-separator-fn token code)]
    (add-betweens-default token code separation)))

(defn record-separation-fn [token code]
  (let [[start-index end-index] (insta/span token)]
    (fn [start end]
      {:pre [(integer? start) (integer? end) <= start end]} 
      (let [opening (= start start-index)
            closing (= end end-index)
            edge (or opening closing)
            sep (subs code (if opening (inc start) start)
                           (if closing (dec end) end))]
        sep))))

(defmethod add-betweens :record [token code]
  (let [separation (record-separation-fn token code)]                                                      
    (add-betweens-default token code separation)))

(defn prop-separation-fn [code [start-index end-index]]
  ;; Strip away the colon as token may be used elsewhere, added at serialization
  (fn [start end]
    (let [opening (= start start-index)
          closing (= end end-index)
          sep (subs code start end)]
      (if-not (or opening closing)
        (if (some-> sep (str/starts-with? ":"))
          (subs sep 1)
          sep)))))

(defmethod add-betweens :prop [token code]
  (let [[start-index end-index] (insta/span token)
        shorthand? (nil? (nth token 2 nil))
        separation (prop-separation-fn code [start-index end-index])
        shorthand-separation (fn [start end] nil)]
      (if shorthand?
        (add-betweens-default 
          (assoc-in token [2] 
                    (with-meta (second token) nil))
          code shorthand-separation) 
        (add-betweens-default token
           code separation))))

(defmethod add-betweens :<> [token code]
  (let [separation (list-item-separator-fn token code)]
    (add-betweens-default token code separation)))

(defn add-between-to-metadata [token code]
  (add-betweens token code))
