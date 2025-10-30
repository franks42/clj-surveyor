;; clj-surveyor runtime introspection (Phase 0)
;; Provides functions to discover vars, dependencies, and metadata from the running system.

(ns clj-surveyor.runtime
  (:require
    [clj-ns-browser.utils :as u]
    [clj-kondo.core :as kondo]
    [clj-surveyor.middleware :as mw]))

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

(defn analyze-namespace-code
  "Use clj-kondo to analyze a namespace's code and return var usage information.
   Uses code captured by middleware or reads from file."
  [ns-obj]
  (try
    (let [ns-name (ns-name ns-obj)
          ;; First try to get captured code from middleware
          code-str (or (mw/get-namespace-code ns-name)
                      ;; Fallback: try to read from file if vars have :file metadata
                      (when-let [sample-var (first (vals (ns-interns ns-obj)))]
                        (when-let [file (:file (meta sample-var))]
                          (when (and (not= file "NO_SOURCE_PATH")
                                    (.exists (java.io.File. file)))
                            (slurp file)))))]
      ;; Analyze with clj-kondo if we have code
      (when code-str
        ;; clj-kondo expects stdin input, so we write to a temp file
        (let [temp-file (java.io.File/createTempFile "clj-surveyor-" ".clj")
              _ (spit temp-file code-str)
              result (kondo/run! {:lint [(.getAbsolutePath temp-file)]
                                  :config {:output {:analysis {:var-usages true
                                                               :var-definitions true}}}})]
          (.delete temp-file)
          (:analysis result))))
    (catch Exception _
      nil)))

(defn find-var-usages
  "Find all vars that reference the target var using clj-kondo analysis.
   
   Phase 0 approach: Analyze namespace code with clj-kondo.
   LIMITATION: Requires source code - works for file-based or middleware-tracked code.
   Returns seq of {:user-fqn ... :ref-type ... :from-var ... :to-var ...}
   Confidence: :high (static analysis)"
  [target-var]
  (let [target-fqn (var-fqn target-var)
        target-ns (.ns target-var)
        target-name (symbol (name target-fqn))]
    ;; Analyze the namespace with clj-kondo
    (when-let [analysis (analyze-namespace-code target-ns)]
      (let [var-usages (:var-usages analysis)]
        (->> var-usages
             (filter (fn [usage]
                       ;; Match usages of our target var
                       (and (= (:name usage) target-name)
                            (= (:to usage) (ns-name target-ns)))))
             (map (fn [usage]
                    {:user-fqn (str (:from usage) "/" (:from-var usage))
                     :ref-type :call
                     :from-var (:from-var usage)
                     :to-var (:name usage)
                     :row (:row usage)
                     :col (:col usage)}))
             (take 100)))))) ;; Limit for Phase 0

(defn get-var-dependents
  "Get all vars that depend on (reference) the target var.
   Args: target-var-fqn (string like 'clojure.core/map')
   Returns: {:target-var ... :dependents [...] :count N :confidence :low|:medium|:high :method ...}"
  [target-var-fqn]
  (if-let [target-var (u/resolve-fqname target-var-fqn)]
    (let [deps (find-var-usages target-var)]
      {:target-var target-var-fqn
       :dependents deps
       :count (count deps)
       :confidence :high  ;; clj-kondo static analysis
       :method :clj-kondo-analysis})
    {:target-var target-var-fqn
     :error "Could not resolve var"
     :dependents []
     :count 0}))

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
