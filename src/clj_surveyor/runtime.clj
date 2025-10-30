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
        ;; Use stdin to avoid temp file - clj-kondo reads from *in* when linting "-"
        (with-in-str code-str
          (let [result (kondo/run! {:lint ["-"]
                                    :config {:output {:analysis {:var-usages true
                                                                 :var-definitions true}}}})]
            (:analysis result)))))
    (catch Exception _
      nil)))

(defn find-var-usages
  "Find all vars that reference the target var using clj-kondo analysis.
   
   Phase 0 approach: Analyze namespace code with clj-kondo.
   LIMITATION: Requires source code - works for file-based or middleware-tracked code.
   Returns seq of {:user-fqn ... :ref-type ... :from-var ... :to-var ...}
   Confidence: :high (static analysis)"
  [target-var]
  (let [target-ns (.ns target-var)
        target-name (.sym target-var)]  ;; Just the symbol name, e.g. 'helper-fn
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

(defn build-dependency-graph
  "Analyze all loaded namespaces and build a complete dependency graph.
   Returns a map of {var-fqn {:dependents #{...} :dependencies #{...}}}.
   
   Phase 0 approach: Uses clj-kondo to analyze all loaded namespaces.
   Only includes vars that have source code available (file-based or middleware-captured).
   
   Options:
   - :ns-filter - regex pattern to filter namespaces (default: analyze all)
   
   Example:
   (build-dependency-graph)
   (build-dependency-graph {:ns-filter #\"^my-project\"})"
  ([] (build-dependency-graph {}))
  ([{:keys [ns-filter]}]
   (let [all-namespaces (if ns-filter
                          (filter #(re-find ns-filter (str (ns-name %))) (all-ns))
                          (all-ns))
         ;; Analyze each namespace and collect var-usages
         ns-analyses (keep (fn [ns-obj]
                            (when-let [analysis (analyze-namespace-code ns-obj)]
                              {:ns (ns-name ns-obj)
                               :var-usages (:var-usages analysis)}))
                          all-namespaces)]
     ;; Process all var-usages to build bidirectional dependency graph
     (reduce (fn [graph {:keys [var-usages]}]
               (reduce (fn [g usage]
                         (let [caller-fqn (str (:from usage) "/" (:from-var usage))
                               callee-fqn (str (:to usage) "/" (:name usage))]
                           (-> g
                               ;; Add to dependents of callee (who calls me?)
                               (update-in [callee-fqn :dependents] (fnil conj #{}) caller-fqn)
                               ;; Add to dependencies of caller (who do I call?)
                               (update-in [caller-fqn :dependencies] (fnil conj #{}) callee-fqn))))
                       graph
                       var-usages))
             {}
             ns-analyses))))

(defn get-cascade-impact
  "Find all vars transitively affected by changing the target var.
   'If I change this function, what breaks?'
   
   Returns a map with:
   - :target-var - the var being changed
   - :cascade - seq of {:var fqn :depth N :path [...]} for each affected var
   - :total-impact - total count of affected vars
   - :max-depth - deepest level of cascade
   
   Options:
   - :max-depth - limit cascade depth (default 10 to prevent infinite loops)
   - :graph - pre-built dependency graph (will build if not provided)
   - :ns-filter - namespace filter for graph building
   
   Example:
   (get-cascade-impact \"my.ns/foo\")
   => {:target-var \"my.ns/foo\"
       :cascade [{:var \"my.ns/bar\" :depth 1 :path [\"my.ns/foo\" \"my.ns/bar\"]}
                 {:var \"my.ns/baz\" :depth 2 :path [\"my.ns/foo\" \"my.ns/bar\" \"my.ns/baz\"]}]
       :total-impact 2
       :max-depth 2}"
  ([target-var-fqn] (get-cascade-impact target-var-fqn {}))
  ([target-var-fqn {:keys [max-depth graph ns-filter] :or {max-depth 10}}]
   (let [dep-graph (or graph (build-dependency-graph (when ns-filter {:ns-filter ns-filter})))
         ;; Breadth-first traversal to find all affected vars with depth tracking
         cascade (loop [to-visit [{:var target-var-fqn :depth 0 :path [target-var-fqn]}]
                       visited #{}
                       results []]
                  (if (empty? to-visit)
                    results
                    (let [{:keys [var depth path] :as current} (first to-visit)
                          rest-to-visit (rest to-visit)]
                      (if (or (visited var) (>= depth max-depth))
                        ;; Already visited or max depth reached - skip
                        (recur rest-to-visit visited results)
                        ;; Find dependents and add to queue
                        (let [dependents (get-in dep-graph [var :dependents] #{})
                              new-depth (inc depth)
                              new-to-visit (map (fn [dep]
                                                 {:var dep
                                                  :depth new-depth
                                                  :path (conj path dep)})
                                               dependents)
                              new-results (if (pos? depth)  ;; Don't include the target itself
                                           (conj results current)
                                           results)]
                          (recur (concat rest-to-visit new-to-visit)
                                (conj visited var)
                                new-results))))))]
     {:target-var target-var-fqn
      :cascade cascade
      :total-impact (count cascade)
      :max-depth (if (empty? cascade) 0 (apply max (map :depth cascade)))})))

(defn print-cascade
  "Pretty-print cascade impact in a tree structure.
   Shows the ripple effect visually with indentation."
  [cascade-result]
  (let [{:keys [target-var cascade total-impact max-depth]} cascade-result]
    (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    (println "CASCADE IMPACT ANALYSIS")
    (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    (println (str "Target: " target-var))
    (println (str "Total Impact: " total-impact " vars affected"))
    (println (str "Max Depth: " max-depth " levels"))
    (println "━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
    (if (zero? total-impact)
      (println "✓ No cascade impact - safe to change!")
      (do
        (println "\nRipple Effect:")
        (doseq [{:keys [var depth]} (sort-by (juxt :depth :var) cascade)]
          (let [indent (apply str (repeat (* 2 depth) " "))
                arrow (if (= depth 1) "→" "↳")]
            (println (str indent arrow " " var " (depth " depth ")"))))
        (println "\n" total-impact "vars would be affected by this change.")))))

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
  
  ;; Find dependents of a specific var
  (get-var-dependents "clojure.core/map")
  
  ;; Build complete dependency graph
  (def graph (build-dependency-graph))
  (get graph "test.demo-file/helper-fn")
  ;; => {:dependents #{"test.demo-file/caller-1" "test.demo-file/caller-2"}
  ;;     :dependencies #{}}
  
  ;; Build graph for specific namespaces only
  (build-dependency-graph {:ns-filter #"^clj-surveyor"})
  
  ;; Cascade impact analysis - "what breaks if I change this?"
  (def impact (get-cascade-impact "test.demo-file/helper-fn"))
  (print-cascade impact)
  ;; Shows all vars affected transitively
  
  ;; See the scary truth about core functions
  (print-cascade (get-cascade-impact "clojure.core/map" {:ns-filter #"^clj-surveyor"}))
  
  ;; Get namespace summary
  (namespace-summary "clojure.core")
  
  ;; Runtime snapshot
  (runtime-snapshot)
  )
