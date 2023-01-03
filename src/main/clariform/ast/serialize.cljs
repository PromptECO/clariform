(ns clariform.ast.serialize
  (:require
   [clojure.spec.alpha :as spec]
   [clojure.string :as string]
   [taoensso.timbre :as timbre]
   [clariform.token :as token]
   [clariform.ast.between :as between
     :refer [ToplevelSeparator Separator]]))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defprotocol Output 
  (stringify [_] 
    "String representation of token in canonical Clarity for code generation")
  (format [_ options]
    "Formatted string representation depending on formatting options"))

(defn output? [inst]
  (satisfies? Output inst))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn spacing-segments [s]
  "Spacing as a sequence of strings separating comments, other whitespace, and special tokens (colon and comma)"
  (loop [s s
         result []]
    (if (empty? s)
      result
      (case (first s)
        (";") 
        (let [seg (first (re-seq #";[^\\n]*[\\n]?" s))]
          (recur
            (subs s (count seg))
            (conj result seg)))
        (":" ",") 
        (recur 
          (subs s 1)
          (conj result (first s)))
        (let [seg (first (re-seq #"[^\\n]*[\\n]?" s))]
          (recur 
            (subs s (max 1 (count seg)))
            (conj result seg)))))))

(defn toplevel-gap [gap]
  "Trim toplevel gap(s) down to max two newlines and no invisible spaces"
  (string/replace gap #"\n[\s]+\n" "\n\n"))

(defn gap-lines [gap]
  ;; split-lines ignores trailing newlines
  (string/split gap #"\r?\n" -1))

(defn trim-lines [gap]
  "Remove blanks before newlines"
  (let [lines (gap-lines gap)] 
    (string/join "\n"
      (concat (some->> 
                (butlast lines)
                (map string/trimr))
              (take-last 1 lines)))))

(defn get-spacing [form where & [post-process]]
  (let [post-process (or post-process identity)]
    (some-> form meta (get-in [:between where]) post-process)))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn retain-spacing [mode front back]
  "Retain explicit visible spacing unless excessive"
  (let [pre (get-spacing front :after stringify)
        sep (get-spacing back :before stringify)
        gap (trim-lines (str pre sep))
        edge (or (nil? front) (nil? back))]
    (case mode 
      (:toplevel)
      (if-not edge
        (if (empty? gap) "\n\n" (toplevel-gap gap)) 
        (if (string/blank? gap) "" (toplevel-gap gap)))
      (:list :<>)
      (if (empty? gap) 
        (if-not edge " ")
        gap)
      ;; TODO: retain spacing
      (:prop) 
      (cond 
        edge gap
        (empty? gap) ": "
        (string/blank? gap) ": "
        (= gap ":") ": "
        (string/blank? (first gap)) (str ":" gap)
        (string/starts-with? gap ":") gap
        :else (str ": " gap))
      (:record) 
      (cond
        (nil? front) 
        (->> (spacing-segments gap)
             (remove (partial = ","))
             (apply str))
        (nil? back) 
        gap 
        (empty? gap) ", "
        (string/blank? gap) 
        (str "," gap)
        (= gap ",") 
        ", "
        (string/starts-with? gap ",") 
        gap
        :else (str ", " gap))
      (do (timbre/warn "Cannot retain spacing for:" mode)
        ""))))

(defn align-spacing [mode front back]
  "Left-align by stripping whitespace after newline (roundtripable)"
  (let [pre (get-spacing front :after stringify)
        sep (get-spacing back :before stringify)
        align (fn [s]
                (string/replace s #"\n[ ]+" "\n"))
        gap (align (trim-lines (str pre sep)))
        edge (or (nil? front) (nil? back))]
    (case mode 
      (:toplevel)
      (if-not edge
        (if (empty? gap) "\n\n" (toplevel-gap gap)) 
        (if (string/blank? gap) "" (toplevel-gap gap)))
      (:list :<>)
      (if (empty? gap) 
        (if-not edge " ") 
        gap)
      ;; TODO: retain spacing
      (:prop) 
      (if-not edge 
         (cond (empty? gap) ": "
               (string/blank? gap) ": "
               (= gap ":") ": "
               (string/blank? (first gap)) (str ":" gap)
               (string/starts-with? gap ":") gap
               :else (str ": " gap))
         gap)
      (:record) 
      (if-not edge
        (cond (empty? gap) ", "
              (string/blank? gap) (str "," gap)
              (= gap ",") ", "
              :else gap)
        gap))))

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

(defn gap-offset [gap offset]
  "Returns updated horizontal offset minding the gap between forms"
  ;; TODO optimize by avoding splitting lines?
  (let [n (and gap (string/last-index-of gap \newline))]
    (if n
      (- (count gap) n 1)
      (+ offset (count gap)))))

(defn format-separated-form [spacing [front back] {:keys [layout adjust-close offset tab] :as options}]
  "Prefix a form with appropriate separation"
  (let [gap (spacing front back)
        offset (gap-offset gap offset)
        options (assoc options :offset offset)]
    (if (some? back)
      (str gap (format back options))
      (if adjust-close
        (if-let [n (and gap (string/last-index-of gap \newline))]
          (apply str (subs gap 0 (inc n)) 
                     (repeat tab \space))
          gap)
        gap))))

(defn format-separated-items [mode forms & [{:keys [layout tab offset] :as options}]]
  {:post [string?]}
  "Separate the items with whitespace consistent with the expected layout"
  (let [spacing (-> (case layout
                      ("retain") retain-spacing
                      ("align") align-spacing
                      ("auto") align-spacing
                      ("compact") compact-spacing)
                    (partial mode))]
    (case layout
      ("retain" "auto")
      (loop [offset (or offset 0)
             result []
             pairs (partition-all 2 1 (list* nil forms))]          
        (if (empty? pairs)
          (apply str result)
          (let [segment (format-separated-form spacing (first pairs) 
                                 (assoc options :offset offset))]
            (recur
              (gap-offset segment offset)
              (conj result segment)                                    
              (rest pairs)))))
      ("align" "compact")
      (apply str 
        (mapcat 
         (partial format-separated-form spacing)
         (partition-all 2 1 (list* nil forms))
         (repeat options))))))

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
     (assoc options :tab (get options :offset 0)
                    :offset (inc (get options :offset 0))))
    ")"))
 
(defmethod format-form :record [form options]
  (str "{" 
    (format-separated-items :record 
                            (token/content form)
                            (assoc options :tab (get options :offset 0)
                                           :offset (inc (get options :offset 0))))
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

(defmethod format-form :skip [form options]
  ;; Ignoring skipped forms for robustness
  "")

(defmethod format-form :toplevel [form options]
  (format-separated-items :toplevel (token/content form) options))

(defmethod format-form :S [form options]
  (format-separated-items :toplevel (token/content form) options))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(extend-type default
  Output
  (stringify [tree]
    (format tree nil))
  (format [tree options]
    (format-form tree options)))

(extend-type Separator
  Output
  (format [self options] (or (:text self) " ")))

(extend-type ToplevelSeparator
  Output
  (stringify [self] (:sep self))
  (format [self {:keys [layout] :as options}]
    (let [{edge :edge sep :sep} self]
      (cond 
        (= layout "compact") (if edge "" "\n")
        (empty? sep) (if edge "" "\n\n")
        (string/blank? sep) (if edge "" sep)
        :else sep))))

;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;;

(defn format-retain [ast]
  (format-form ast {:layout "retain"}))

(defn format-adjust [ast]
  (format-form ast {:layout "retain" :adjust-close true}))

(defn format-align [ast]
  (format-form ast {:layout "align"}))

(defn format-auto [ast]
  (format-form ast {:layout "auto"}))

(defn format-compact [ast]
  (format-form ast {:layout "compact"}))

