(ns cljs-node-io.url
  "Extend pkpkpk/cljs-node-io with url based io (experimental)"
  (:require
    [cljs.core.async :as async
     :refer [go chan put! close!]]
    ["https" :as https]
    [cljs-node-io.async :as node-async]
    [cljs-node-io.core :as node-io
     :refer [slurp]]))

(defn aslurp [resource]
  (if (and (cljs.core/instance? goog.Uri resource)
           (not= "file" (.getScheme ^js resource)))
    (let [req (https/get (str resource))
          event-chan (chan)]          
      (.on ^js req "response"
           (fn [response]
             ;; NOTE using .-Readable just to work around optimization issue 
             ;; in node-cljs-io 2.0.332 reported as "Right-hand side of 'instanceof' is not an object"
             (when (instance? (.-Readable cljs-node-io.async/stream) response)
               (node-async/readable-onto-ch response event-chan))))
      (async/map (fn [[tag & [payload] :as event]]
                   (case tag
                     :data (apply str payload)
                     :error (ex-info "HTTP get failed." {:payload payload} :io)
                     :end nil
                     tag))  
                 [event-chan]))
    (go (node-io/slurp resource))
    #_(node-io/aslurp resource)))
