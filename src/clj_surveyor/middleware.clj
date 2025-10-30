;; clj-surveyor nREPL middleware (Phase 0 stub)
;; Provides runtime introspection helpers and hooks for the surveyor entity graph.

(ns clj-surveyor.middleware
  (:require
    [clojure.tools.logging :as log]))

;; Phase 0: Minimal middleware stub for development
;; Will be expanded with Datascript entity graph, var tracking, etc.

(defn wrap-surveyor
  "nREPL middleware that adds surveyor runtime introspection.
   Phase 0 stub: logs eval events and provides helper ops.
   Future: maintain Datascript entity graph, track var changes, provide staleness queries."
  [handler]
  (fn [{:keys [op code ns] :as msg}]
    ;; Log eval events for Phase 0 development visibility
    (when (= op "eval")
      (log/debug (format "surveyor: eval in ns=%s code=%s" ns (pr-str code))))
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
