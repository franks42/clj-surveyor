;; clj-surveyor runtime introspection (Phase 0)
;; Provides functions to discover vars, dependencies, and metadata from the running system.

(ns clj-surveyor.runtime
  (:require
    [clj-ns-browser.utils :as browser-utils]))

;; Phase 0: Basic runtime introspection using clj-ns-browser utilities

(defn all-vars
  "Get all vars in the runtime as a seq of var objects.
   Optionally filter by namespace name pattern (regex)."
  ([]
   (mapcat ns-interns (all-ns)))
  ([ns-pattern]
   (let [pattern (re-pattern ns-pattern)
         matching-nses (filter #(re-find pattern (str (ns-name %))) (all-ns))]
     (mapcat (comp vals ns-interns) matching-nses))))

(defn var-fqn
  "Get fully qualified name for a var."
  [v]
  (when (var? v)
    (str (ns-name (.ns v)) "/" (.sym v))))

(defn var-info
  "Extract basic info about a var for Phase 0.
   Returns a map with :fqn, :name, :ns, :meta, :value-type."
  [v]
  (when (var? v)
    (let [m (meta v)]
      {:fqn (var-fqn v)
       :name (.sym v)
       :ns (ns-name (.ns v))
       :meta m
       :macro? (:macro m)
       :private? (:private m)
       :dynamic? (boolean (and (:dynamic m) (bound? v)))
       :value-type (when (bound? v)
                     (try
                       (type @v)
                       (catch Throwable _ nil)))})))

(defn find-var-usages
  "Find where a var is used by scanning the source of other vars.
   Phase 0 stub: only checks if var is referenced in other var metadata.
   Returns seq of {:user-fqn :ref-type} maps."
  [target-var]
  (let [target-sym (.sym target-var)]
    (->> (all-vars)
         (keep (fn [v]
                 (let [m (meta v)]
                   ;; Check :arglists, :doc for mentions (crude heuristic for Phase 0)
                   (when (or (and (:doc m) (re-find (re-pattern (str target-sym)) (:doc m)))
                             (some #(some #{target-sym} (flatten %)) (:arglists m)))
                     {:user-fqn (var-fqn v)
                      :ref-type :possible-usage}))))
         (take 100)))) ;; Limit for Phase 0

(defn get-var-dependents
  "Get vars that likely depend on the target var.
   Phase 0: Uses metadata scanning. Future: static analysis + runtime tracking."
  [var-name]
  (when-let [v (browser-utils/resolve-fqname var-name)]
    (when (var? v)
      {:target-var (var-fqn v)
       :dependents (find-var-usages v)
       :count (count (find-var-usages v))
       :confidence :low  ;; Phase 0 uses heuristics only
       :method :metadata-scan})))

(defn namespace-summary
  "Get summary of a namespace: var count, public/private breakdown, types."
  [ns-name]
  (when-let [ns (find-ns (symbol ns-name))]
    (let [all-vars (vals (ns-interns ns))
          publics (vals (ns-publics ns))
          privates (filter (comp :private meta) all-vars)]
      {:namespace (ns-name ns)
       :var-count (count all-vars)
       :public-count (count publics)
       :private-count (count privates)
       :macros (count (filter (comp :macro meta) all-vars))
       :functions (count (filter (comp fn? var-get) all-vars))
       :dynamics (count (filter (comp :dynamic meta) all-vars))})))

(defn runtime-snapshot
  "Capture a snapshot of the current runtime state.
   Phase 0: Basic counts and namespace list."
  []
  {:timestamp (java.util.Date.)
   :namespace-count (count (all-ns))
   :namespaces (map ns-name (all-ns))
   :total-vars (count (all-vars))
   :surveyor-version "0.4.0-phase0"})

(comment
  ;; Phase 0 usage examples:
  
  ;; Get all vars in clojure.core
  (take 5 (all-vars "clojure.core"))
  
  ;; Get info about map
  (var-info #'clojure.core/map)
  
  ;; Find dependents of map (Phase 0 heuristic)
  (get-var-dependents "clojure.core/map")
  
  ;; Get namespace summary
  (namespace-summary "clojure.core")
  
  ;; Runtime snapshot
  (runtime-snapshot)
  )
