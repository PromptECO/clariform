(ns clariform.ast.escape
  "Converting between escaped Clarity strings and native strings"
  (:require 
   [goog.i18n.uChar :as u]
   [clojure.string :as string]))

(defn ^string escape-unicode [s cmap]
  "Return a new string, using cmap to escape each codepoint (similar to clojure.string/escape)"
  ;; like https://cljs.github.io/api/clojure.string/escape but unicode
   (let [buffer (goog.string/StringBuffer.)
         length (.-length s)]
     (loop [index 0]
       (if (== length index)
         (. buffer (toString))
         (let [ch (u/getCodePointAround s (int index))
               replacement (cmap ch)]
           (if-not (nil? replacement)
             (.append buffer (str replacement))
             (if (pos? ch)
               (.append buffer (.fromCodePoint js/String ch))))
           (recur (inc index)))))))

(def ^{:doc "Returns Clarity escape string for char or nil if none, like char-escape-string but takes charcode"}
  clarity-charcode-escape
  ;;like https://clojuredocs.org/clojure.core/char-escape-string 
  {(u/getCodePointAround \\ 0) "\\\\"
   (u/getCodePointAround \" 0) "\\\""
   10 "\\n"
   9 "\\t"
   13 "\\r"
   0 "\\0"})
             
(defn ^string clarity-escape-string [s & {:keys [unicode-only]}]
  "Escape encoded string for Clarity"
  (escape-unicode s
    (fn [code]
      (or (and (not unicode-only)
               (clarity-charcode-escape code))
          (if (> code 127)
            (str "\\u{" 
                 (-> (.toString code 16)
                     (string/upper-case))
                "}")))))) 

(defn ascii-string? [s]
  {:pre [(string? s)]}
  "Truthy if string only contains ascii characters (may still be unicode escaped)"
  ;; u/getCodePointAround
  (every? #(<= (.charCodeAt % 0) 255) s))

(defn unicode-escaped? [s]
  "Truthy if Clarity-escaped string contains unicode escapes"
  ;; NOTE should not be confused by \\u which is not start of unicode escape but two characters
  (->> (string/replace s "\\\\" "")
       (re-find #"\\u\{([\dA-F]*)\}")))
