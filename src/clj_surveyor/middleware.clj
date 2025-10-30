;; clj-surveyor nREPL middleware (Phase 0 stub)
;; Provides runtime introspection helpers and hooks for the surveyor entity graph.

(ns clj-surveyor.middleware
  (:require
    [clojure.string :as str]
    [clojure.tools.logging :as log]))

;; Phase 0: Capture source code for later analysis
;; Store code strings by namespace for clj-kondo analysis

(defonce namespace-code-store
  (atom {}))

(defn get-namespace-code
  "Get all stored code for a namespace as a single string."
  [ns-name]
  (let [codes (get @namespace-code-store ns-name)]
    (when (seq codes)
      (str "(ns " ns-name ")\n\n"
           (str/join "\n\n" codes)))))

(defn clear-namespace-code
  "Clear stored code for a namespace (e.g., when file is reloaded)."
  [ns-name]
  (swap! namespace-code-store dissoc ns-name))

(defn get-all-namespace-codes
  "Get all stored code organized by namespace."
  []
  @namespace-code-store)

(defn wrap-surveyor
  "nREPL middleware that adds surveyor runtime introspection.
   Phase 0: Captures eval'd code for dependency analysis.
   Future: maintain Datascript entity graph, track var changes, provide staleness queries."
  [handler]
  (fn [{:keys [op code ns] :as msg}]
    ;; Capture eval'd code for dependency analysis
    (when (and (= op "eval") code ns)
      (log/debug (format "surveyor: capturing code in ns=%s" ns))
      (swap! namespace-code-store update (symbol ns) (fnil conj []) code))
    ;; Pass through to next handler
    (handler msg)))

(defn surveyor-middleware-descriptor
  "nREPL middleware descriptor for clj-surveyor."
  []
  {:requires #{}
   :expects #{"eval"}
   :handles {}})

(comment
  ;; Phase 0 usage example:
  ;; Add to nREPL server startup:
  ;; (require '[nrepl.server :as nrepl])
  ;; (require '[clj-surveyor.middleware :as surveyor-mw])
  ;; (nrepl/start-server :port 7888
  ;;                     :handler (nrepl/default-handler #'surveyor-mw/wrap-surveyor))
  
  ;; Future Phase 1+: Add ops for:
  ;; - "surveyor-dependents" - return dependents of a var
  ;; - "surveyor-staleness" - return staleness info for a var
  ;; - "surveyor-cascade" - return cascade impact for an eval
  ;; - "surveyor-snapshot" - return current entity graph snapshot
  )
