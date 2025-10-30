# clj-surveyor: Implementation Guide

**Purpose**: This document provides concrete implementation details for collecting runtime information and storing it in the Datascript entity graph, bridging the design concepts in `design-document-v3.md` to actual Clojure code.

**Target Environment**: JVM Clojure with nREPL server

---

## Phase 0 Target Environment

**Primary Target**: JVM Clojure with nREPL server running

**Why nREPL from the start:**
- nREPL clients readily available (CIDER, Calva, Cursive, Conjure)
- Built-in infrastructure for middleware and message handling
- Can capture eval events automatically (not just manual queries)
- Foundation for Phase 2 editor integration

**Architecture:**
```
┌─────────────────────────────────────────┐
│  Editor (Emacs/VSCode/IntelliJ)         │
│  nREPL Client                           │
└──────────────┬──────────────────────────┘
               │ nREPL protocol
               ▼
┌─────────────────────────────────────────┐
│  nREPL Server                           │
│  ┌───────────────────────────────────┐  │
│  │ clj-surveyor Middleware           │  │
│  │ - Intercept eval messages         │  │
│  │ - Record var changes              │  │
│  │ - Update entity graph             │  │
│  └───────────────────────────────────┘  │
│  ┌───────────────────────────────────┐  │
│  │ Datascript Database               │  │
│  │ - Var entities                    │  │
│  │ - Dependencies                    │  │
│  │ - Events                          │  │
│  └───────────────────────────────────┘  │
└─────────────────────────────────────────┘
```

**Phase 0 User Experience:**

1. Add to project dependencies:
   ```clojure
   ;; deps.edn
   {:deps {clj-surveyor/clj-surveyor {:mvn/version "0.1.0"}}}
   ```

2. Start nREPL with middleware:
   ```clojure
   ;; In your project or user profile
   (require '[clj-surveyor.nrepl :as surveyor-nrepl])
   
   ;; Start nREPL with surveyor middleware
   (surveyor-nrepl/start-server {:port 7888})
   
   ;; Or add to existing nREPL
   (surveyor-nrepl/inject-middleware!)
   ```

3. Connect from editor, then query:
   ```clojure
   (require '[clj-surveyor.core :as survey])
   (survey/dependents 'my.app/process-user)
   ;; => Prints dependency tree with confidence indicators
   
   ;; Middleware automatically tracks changes in background
   ```

---

## Table of Contents

1. [Runtime Information Collection](#runtime-information-collection)
2. [Datascript Schema Definition](#datascript-schema-definition)
3. [Entity Construction Recipes](#entity-construction-recipes)
4. [Change Detection Mechanisms](#change-detection-mechanisms)
5. [File Analysis Integration](#file-analysis-integration)
6. [Performance Considerations](#performance-considerations)

---

## Runtime Information Collection

### 1. Discovering All Vars in Runtime

**Goal**: Get FQNs for all vars currently loaded in the Clojure runtime.

```clojure
(ns clj-surveyor.collect.vars
  (:require [clojure.repl :as repl]))

(defn all-namespaces
  "Get all loaded namespaces."
  []
  (all-ns))

(defn namespace-vars
  "Get all vars in a namespace."
  [ns-obj]
  (vals (ns-interns ns-obj)))

(defn all-runtime-vars
  "Collect all vars from all loaded namespaces."
  []
  (mapcat namespace-vars (all-namespaces)))

(defn var->fqn
  "Convert var to fully-qualified name symbol."
  [v]
  (symbol (str (ns-name (:ns (meta v))))
          (str (:name (meta v)))))

(defn all-var-fqns
  "Get FQNs for all runtime vars."
  []
  (map var->fqn (all-runtime-vars)))

;; Example usage:
;; (all-var-fqns)
;; => (clojure.core/map clojure.core/filter my.app/process-user ...)
```

### 2. Extracting Var Metadata

**Goal**: For each var, extract all available metadata.

```clojure
(defn var-metadata
  "Extract comprehensive metadata from a var."
  [v]
  (let [m (meta v)
        ns-name (ns-name (:ns m))
        var-name (:name m)]
    {:fqn (symbol (str ns-name) (str var-name))
     :ns ns-name
     :name (str var-name)
     :file (:file m)
     :line (:line m)
     :column (:column m)
     :doc (:doc m)
     :arglists (:arglists m)
     :macro? (boolean (:macro m))
     :private? (boolean (:private m))
     :dynamic? (boolean (:dynamic m))
     :added (:added m)
     :deprecated (:deprecated m)
     :tag (:tag m)
     :test (:test m)}))

;; Example:
;; (var-metadata #'clojure.core/map)
;; => {:fqn clojure.core/map
;;     :ns clojure.core
;;     :name "map"
;;     :file "clojure/core.clj"
;;     :line 2745
;;     :arglists ([f] [f coll] [f c1 c2] ...)
;;     :macro? false
;;     :private? false
;;     ...}
```

### 3. Determining Var Types

**Goal**: Classify vars by type (function, macro, value, etc.).

```clojure
(defn var-type
  "Determine the type of a var."
  [v]
  (let [m (meta v)
        val (try @v (catch Exception _ ::unbound))]
    (cond
      (:macro m)           :macro
      (fn? val)            :function
      (= val ::unbound)    :unbound
      (var? val)           :var-holding-var  ; Rare but possible
      (class? val)         :class
      (instance? clojure.lang.MultiFn val) :multimethod
      (instance? clojure.lang.Atom val)    :atom
      (instance? clojure.lang.Ref val)     :ref
      (instance? clojure.lang.Agent val)   :agent
      :else                :value)))

(defn var-value-summary
  "Safe summary of var value (for debugging, not storage)."
  [v]
  (let [val (try @v (catch Exception e ::error))]
    (cond
      (= val ::error) "<error dereferencing>"
      (fn? val) "<function>"
      (macro? val) "<macro>"
      (> (count (pr-str val)) 100) (str (subs (pr-str val) 0 100) "...")
      :else (pr-str val))))
```

### 4. Extracting Source Code

**Goal**: Get the source code for a var if available.

```clojure
(require '[clojure.repl :as repl])

(defn var-source
  "Get source code for a var, if available."
  [v]
  (try
    (repl/source-fn (var->fqn v))
    (catch Exception _ nil)))

;; Alternative: Use clojure.java.io to read from file
(require '[clojure.java.io :as io])

(defn var-source-from-file
  "Read source from file using line/column metadata."
  [v]
  (let [m (meta v)
        file (:file m)
        line (:line m)]
    (when (and file line)
      (try
        (with-open [rdr (io/reader (io/resource file))]
          ;; This is simplified - real implementation needs to parse forms
          (nth (line-seq rdr) (dec line)))
        (catch Exception _ nil)))))
```

### 5. Finding Dependencies (What This Var Uses)

**Goal**: Determine which other vars/namespaces this var depends on.

**Approach 1: Static Analysis via tools.analyzer**

```clojure
(require '[clojure.tools.analyzer.jvm :as ana])

(defn analyze-var-dependencies
  "Use tools.analyzer to find var dependencies."
  [v]
  (let [source (var-source v)]
    (when source
      (try
        (let [analyzed (ana/analyze (read-string source))]
          ;; Extract :var nodes from AST
          (extract-var-refs analyzed))
        (catch Exception _ #{})))))

(defn extract-var-refs
  "Walk AST and extract all var references."
  [ast]
  (letfn [(walk [node]
            (cond
              (and (map? node) (= :var (:op node)))
              #{(:var node)}
              
              (map? node)
              (reduce into #{} (map walk (vals node)))
              
              (coll? node)
              (reduce into #{} (map walk node))
              
              :else
              #{}))]
    (walk ast)))
```

**Approach 2: Simpler - Extract symbols from source**

```clojure
(defn extract-symbols-from-source
  "Extract all symbols from source code (approximate)."
  [source-str]
  (when source-str
    (let [forms (try (read-string (str "[" source-str "]"))
                     (catch Exception _ []))]
      (set (filter symbol? (tree-seq coll? seq forms))))))

(defn resolve-symbols-to-vars
  "Try to resolve symbols to actual vars in current runtime."
  [ns-context symbols]
  (keep (fn [sym]
          (try
            (ns-resolve ns-context sym)
            (catch Exception _ nil)))
        symbols))

(defn var-dependencies-simple
  "Get dependencies using simple symbol extraction."
  [v]
  (let [source (var-source v)
        ns-obj (:ns (meta v))
        symbols (extract-symbols-from-source source)]
    (set (map var->fqn (resolve-symbols-to-vars ns-obj symbols)))))
```

**Approach 3: Integration with clj-kondo** (Recommended for Phase 1)

```clojure
(require '[clj-kondo.core :as kondo])

(defn analyze-file-with-kondo
  "Use clj-kondo to analyze a file and extract dependencies."
  [file-path]
  (let [result (kondo/run! {:lint [file-path]
                            :config {:output {:analysis true}}})]
    (:analysis result)))

(defn kondo-var-usages
  "Extract var usages from clj-kondo analysis."
  [analysis var-fqn]
  (let [ns (namespace var-fqn)
        name (name var-fqn)
        var-defs (filter #(and (= (:ns %) ns) (= (:name %) name))
                        (:var-definitions analysis))]
    ;; Find all var-usages that reference this var
    (filter #(= (:to %) var-fqn) (:var-usages analysis))))
```

### 6. Finding Dependents (What Uses This Var)

**Goal**: Reverse dependency - what calls/uses this var?

```clojure
(defn find-dependents
  "Find all vars that reference the given var.
   This requires analyzing all other vars in runtime."
  [target-var all-vars]
  (let [target-fqn (var->fqn target-var)]
    (filter (fn [v]
              (let [deps (var-dependencies-simple v)]
                (contains? deps target-fqn)))
            all-vars)))

;; More efficient: Build reverse index once
(defn build-dependency-graph
  "Build full dependency graph for all vars."
  [all-vars]
  (let [deps-map (into {}
                       (map (fn [v]
                              [(var->fqn v) (var-dependencies-simple v)])
                            all-vars))]
    {:forward deps-map
     :reverse (reverse-dependency-map deps-map)}))

(defn reverse-dependency-map
  "Create reverse lookup: var -> vars that depend on it."
  [forward-map]
  (reduce (fn [acc [var deps]]
            (reduce (fn [m dep]
                      (update m dep (fnil conj #{}) var))
                    acc
                    deps))
          {}
          forward-map))
```

### 7. Namespace Information

**Goal**: Collect namespace-level metadata.

```clojure
(defn namespace-metadata
  "Extract namespace information."
  [ns-obj]
  (let [m (meta ns-obj)]
    {:ns (ns-name ns-obj)
     :name (str (ns-name ns-obj))
     :doc (:doc m)
     :author (:author m)
     :file (some-> ns-obj .getName (clojure.string/replace "." "/") (str ".clj"))
     :requires (map first (ns-refers ns-obj))
     :imports (ns-imports ns-obj)
     :aliases (ns-aliases ns-obj)
     :interns (keys (ns-interns ns-obj))
     :publics (keys (ns-publics ns-obj))}))

;; Example:
;; (namespace-metadata (find-ns 'my.app))
;; => {:ns my.app
;;     :name "my.app"
;;     :doc "Main application namespace"
;;     :requires (clojure.string clojure.set ...)
;;     :interns (process-user validate helper ...)
;;     ...}
```

### 8. Detecting File-Backed vs Ephemeral Vars

**Goal**: Determine if var exists in a file or is REPL-only.

```clojure
(require '[clojure.java.io :as io])

(defn file-exists?
  "Check if the file referenced in var metadata exists."
  [file-path]
  (when file-path
    (or (.exists (io/file file-path))
        (io/resource file-path))))

(defn var-file-backed?
  "Check if var has a backing file."
  [v]
  (let [m (meta v)
        file (:file m)]
    (boolean (and file (file-exists? file)))))

(defn var-ephemeral?
  "Check if var is REPL-only (no file backing)."
  [v]
  (not (var-file-backed? v)))

(defn compare-var-to-file
  "Check if REPL var value matches what's in the file."
  [v]
  (let [repl-source (pr-str @v)  ; Current value
        file-source (var-source-from-file v)]
    {:file-backed? (var-file-backed? v)
     :ephemeral? (var-ephemeral? v)
     :diverged? (and file-source
                     (not= repl-source file-source))
     :repl-value repl-source
     :file-value file-source}))
```

---

## Datascript Schema Definition

### Complete Schema

```clojure
(ns clj-surveyor.schema
  (:require [datascript.core :as d]))

(def schema
  {;; Entity identification
   :entity/type      {:db/index true}
   :entity/fqn       {:db/unique :db.unique/identity
                      :db/index true}
   
   ;; Temporal tracking
   :entity/captured-at {:db/index true
                        :db/valueType :db.type/instant}
   :entity/confidence  {:db/index true
                        :db/valueType :db.type/double}
   :entity/staleness-ms {:db/index true
                         :db/valueType :db.type/long}
   :entity/version     {:db/index true
                        :db/valueType :db.type/long}
   
   ;; Var entities
   :var/fqn         {:db/unique :db.unique/identity
                     :db/index true}
   :var/ns          {:db/index true}
   :var/name        {:db/index true}
   :var/file        {}
   :var/line        {:db/valueType :db.type/long}
   :var/column      {:db/valueType :db.type/long}
   :var/doc         {}
   :var/arglists    {:db/cardinality :db.cardinality/many}
   :var/source      {}
   :var/macro?      {:db/valueType :db.type/boolean}
   :var/private?    {:db/valueType :db.type/boolean}
   :var/dynamic?    {:db/valueType :db.type/boolean}
   :var/type        {:db/index true}  ; :function, :macro, :value, etc.
   
   ;; Var relationships
   :var/uses        {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many
                     :db/index true}
   :var/used-by     {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many
                     :db/index true}
   
   ;; Persistence tracking
   :var/file-backed?  {:db/valueType :db.type/boolean
                       :db/index true}
   :var/ephemeral?    {:db/valueType :db.type/boolean
                       :db/index true}
   :var/file-diverged? {:db/valueType :db.type/boolean
                        :db/index true}
   
   ;; Namespace entities
   :ns/name         {:db/unique :db.unique/identity
                     :db/index true}
   :ns/doc          {}
   :ns/file         {}
   :ns/requires     {:db/cardinality :db.cardinality/many}
   :ns/imports      {:db/cardinality :db.cardinality/many}
   :ns/aliases      {:db/cardinality :db.cardinality/many}
   
   ;; Usage entities (relationships)
   :usage/id        {:db/unique :db.unique/identity}
   :usage/caller    {:db/valueType :db.type/ref
                     :db/index true}
   :usage/callee    {:db/valueType :db.type/ref
                     :db/index true}
   :usage/location  {}
   :usage/context   {:db/index true}
   :usage/count     {:db/valueType :db.type/long}
   :usage/frequency-hz {:db/valueType :db.type/double}
   
   ;; Event entities
   :event/id        {:db/unique :db.unique/identity}
   :event/type      {:db/index true}
   :event/target    {:db/valueType :db.type/ref
                     :db/index true}
   :event/timestamp {:db/valueType :db.type/instant
                     :db/index true}
   :event/source    {:db/index true}
   :event/persistent? {:db/valueType :db.type/boolean
                       :db/index true}
   :event/ephemeral?  {:db/valueType :db.type/boolean
                       :db/index true}
   :event/file-backed {}
   
   ;; Event relationships
   :event/triggered-by {:db/valueType :db.type/ref}
   :event/cascade-depth {:db/valueType :db.type/long
                         :db/index true}
   :event/sequence-id   {:db/valueType :db.type/long
                         :db/index true}
   
   ;; File entities
   :file/path       {:db/unique :db.unique/identity
                     :db/index true}
   :file/namespace  {:db/valueType :db.type/ref}
   :file/statements {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many
                     :db/isComponent true}
   :file/last-modified {:db/valueType :db.type/instant
                        :db/index true}
   :file/last-evaled   {:db/valueType :db.type/instant
                        :db/index true}
   :file/partial-eval? {:db/valueType :db.type/boolean}
   
   ;; Statement entities
   :statement/id    {:db/unique :db.unique/identity}
   :statement/file  {:db/valueType :db.type/ref}
   :statement/namespace {:db/valueType :db.type/ref}
   :statement/type  {:db/index true}
   :statement/line  {:db/valueType :db.type/long}
   :statement/source {}
   :statement/defines {:db/valueType :db.type/ref}
   :statement/depends-on {:db/valueType :db.type/ref
                          :db/cardinality :db.cardinality/many}
   :statement/purity {:db/index true}
   :statement/downstream-impact {:db/valueType :db.type/long
                                 :db/index true}
   :statement/position {:db/valueType :db.type/long
                        :db/index true}
   
   ;; Staleness tracking
   :staleness/scope {:db/valueType :db.type/ref}
   :staleness/last-sync {:db/valueType :db.type/instant}
   :staleness/age-ms {:db/valueType :db.type/long}
   :staleness/confidence {:db/valueType :db.type/double}})

(defn create-db
  "Create new Datascript database with schema."
  []
  (d/create-conn schema))
```

### Schema Design Principles

1. **Use `:db/unique :db.unique/identity` for lookups**: `:entity/fqn`, `:var/fqn`, `:ns/name`, `:file/path`
2. **Index frequently-queried attributes**: `:entity/type`, `:var/type`, `:event/type`, timestamps
3. **Use refs for relationships**: `:var/uses`, `:usage/caller`, `:event/target`
4. **Component entities for ownership**: `:file/statements` with `:db/isComponent true`
5. **Explicit types for safety**: `:db.type/instant`, `:db.type/long`, `:db.type/boolean`

---

## Entity Construction Recipes

### Creating Var Entities

```clojure
(defn var->entity
  "Convert a runtime var to a Datascript entity map."
  [v timestamp]
  (let [m (meta v)
        fqn (var->fqn v)
        deps (var-dependencies-simple v)]
    {:entity/type :var
     :entity/fqn fqn
     :entity/captured-at timestamp
     :entity/confidence 1.0
     :entity/staleness-ms 0
     :entity/version 1
     
     :var/fqn fqn
     :var/ns (namespace fqn)
     :var/name (name fqn)
     :var/file (:file m)
     :var/line (:line m)
     :var/column (:column m)
     :var/doc (:doc m)
     :var/arglists (:arglists m)
     :var/source (var-source v)
     :var/macro? (boolean (:macro m))
     :var/private? (boolean (:private m))
     :var/dynamic? (boolean (:dynamic m))
     :var/type (var-type v)
     
     ;; Dependencies (refs will be resolved by Datascript)
     :var/uses (mapv (fn [dep-fqn] [:var/fqn dep-fqn]) deps)
     
     ;; Persistence
     :var/file-backed? (var-file-backed? v)
     :var/ephemeral? (var-ephemeral? v)
     :var/file-diverged? false}))  ; Computed separately

;; Example usage:
;; (var->entity #'my.app/process-user (java.util.Date.))
```

### Creating Usage Entities

```clojure
(defn create-usage-entity
  "Create a usage entity representing caller->callee relationship."
  [caller-fqn callee-fqn location]
  {:usage/id (str caller-fqn "->" callee-fqn)
   :usage/caller [:var/fqn caller-fqn]
   :usage/callee [:var/fqn callee-fqn]
   :usage/location location
   :usage/context :function-body  ; Could be more specific
   :usage/count 1
   :usage/frequency-hz 0.0})
```

### Creating Event Entities

```clojure
(defn create-var-redefinition-event
  "Create an event for var redefinition."
  [var-fqn timestamp source]
  (let [event-id (str "event-" (System/currentTimeMillis) "-" (random-uuid))]
    {:event/id event-id
     :event/type :var-redefinition
     :event/target [:var/fqn var-fqn]
     :event/timestamp timestamp
     :event/source source  ; :repl-eval, :file-load, :ns-reload
     :event/persistent? (= source :file-load)
     :event/ephemeral? (= source :repl-eval)
     :event/sequence-id (next-sequence-id)}))

(def sequence-counter (atom 0))

(defn next-sequence-id []
  (swap! sequence-counter inc))
```

---

## Change Detection Mechanisms

### Using `add-watch` for Var Changes

```clojure
(ns clj-surveyor.watch
  (:require [datascript.core :as d]))

(defn watch-var
  "Add a watch to a var to detect changes."
  [v db-conn]
  (let [fqn (var->fqn v)]
    (add-watch v ::surveyor
      (fn [key ref old-val new-val]
        (when (not= old-val new-val)
          (record-var-change! db-conn fqn old-val new-val))))))

(defn record-var-change!
  "Record a var change event in the database."
  [db-conn fqn old-val new-val]
  (let [timestamp (java.util.Date.)
        event (create-var-redefinition-event fqn timestamp :repl-eval)]
    (d/transact! db-conn [event])
    
    ;; Update var entity
    (d/transact! db-conn
      [{:var/fqn fqn
        :entity/captured-at timestamp
        :entity/version (inc (get-var-version db-conn fqn))}])))

(defn watch-all-vars
  "Add watches to all vars in runtime."
  [db-conn]
  (doseq [v (all-runtime-vars)]
    (watch-var v db-conn)))
```

### Polling for New Namespaces

```clojure
(defn poll-for-new-namespaces
  "Check for newly loaded namespaces."
  [db-conn known-namespaces]
  (let [current-namespaces (set (map ns-name (all-namespaces)))
        new-namespaces (clojure.set/difference current-namespaces known-namespaces)]
    (doseq [ns-name new-namespaces]
      (index-namespace! db-conn (find-ns ns-name)))
    (into known-namespaces new-namespaces)))

(defn start-namespace-poller
  "Start background thread to poll for new namespaces."
  [db-conn interval-ms]
  (let [known (atom #{})]
    (future
      (while true
        (swap! known #(poll-for-new-namespaces db-conn %))
        (Thread/sleep interval-ms)))))
```

---

## File Analysis Integration

### Using clj-kondo for Static Analysis

```clojure
(ns clj-surveyor.kondo
  (:require [clj-kondo.core :as kondo]
            [datascript.core :as d]))

(defn analyze-project
  "Run clj-kondo on entire project."
  [project-path]
  (kondo/run! {:lint [project-path]
               :config {:output {:analysis {:arglists true
                                            :locals true
                                            :keywords true
                                            :var-definitions true
                                            :var-usages true}}}}))

(defn kondo->var-entities
  "Convert clj-kondo var-definitions to Datascript entities."
  [var-defs timestamp]
  (mapv (fn [vd]
          {:entity/type :var
           :entity/fqn (symbol (str (:ns vd)) (str (:name vd)))
           :entity/captured-at timestamp
           :entity/confidence 0.9  ; Static analysis slightly lower confidence
           
           :var/fqn (symbol (str (:ns vd)) (str (:name vd)))
           :var/ns (str (:ns vd))
           :var/name (str (:name vd))
           :var/file (:filename vd)
           :var/line (:row vd)
           :var/column (:col vd)
           :var/doc (:doc vd)
           :var/arglists (:arglist-strs vd)
           :var/macro? (boolean (:macro vd))
           :var/private? (boolean (:private vd))
           :var/file-backed? true
           :var/ephemeral? false})
        var-defs))

(defn kondo->usage-entities
  "Convert clj-kondo var-usages to usage entities."
  [var-usages]
  (mapv (fn [vu]
          {:usage/id (str (:from vu) "->" (:to vu) "-" (:row vu))
           :usage/caller [:var/fqn (:from vu)]
           :usage/callee [:var/fqn (:to vu)]
           :usage/location {:file (:filename vu)
                           :line (:row vu)
                           :column (:col vu)}})
        var-usages))

(defn bootstrap-from-kondo
  "Initialize database from clj-kondo analysis."
  [db-conn project-path]
  (let [analysis (analyze-project project-path)
        timestamp (java.util.Date.)
        var-entities (kondo->var-entities (:var-definitions analysis) timestamp)
        usage-entities (kondo->usage-entities (:var-usages analysis))]
    
    ;; Transact all entities
    (d/transact! db-conn var-entities)
    (d/transact! db-conn usage-entities)))
```

---

## Performance Considerations

### Batching Updates

```clojure
(defn batch-transact
  "Transact entities in batches to avoid overwhelming Datascript."
  [db-conn entities batch-size]
  (doseq [batch (partition-all batch-size entities)]
    (d/transact! db-conn batch)))

;; Example:
;; (batch-transact conn all-var-entities 100)
```

### Incremental Updates

```clojure
(defn incremental-index-namespace
  "Only index vars that changed since last capture."
  [db-conn ns-obj]
  (let [ns-name (ns-name ns-obj)
        existing-vars (get-namespace-vars-from-db db-conn ns-name)
        current-vars (set (map var->fqn (namespace-vars ns-obj)))
        
        ;; Determine changes
        new-vars (clojure.set/difference current-vars existing-vars)
        removed-vars (clojure.set/difference existing-vars current-vars)]
    
    ;; Add new vars
    (doseq [fqn new-vars]
      (index-var! db-conn (resolve fqn)))
    
    ;; Mark removed vars (don't delete - keep history)
    (doseq [fqn removed-vars]
      (d/transact! db-conn [{:var/fqn fqn
                             :var/removed? true}]))))
```

### Caching Expensive Computations

```clojure
(def dependency-cache (atom {}))

(defn cached-dependencies
  "Get dependencies with caching."
  [var-fqn]
  (if-let [cached (@dependency-cache var-fqn)]
    cached
    (let [deps (compute-dependencies var-fqn)]
      (swap! dependency-cache assoc var-fqn deps)
      deps)))

(defn invalidate-dependency-cache
  "Clear cache when vars change."
  [var-fqn]
  (swap! dependency-cache dissoc var-fqn))
```

---

## Bootstrap Sequence (Phase 0 with nREPL)

**Recommended startup sequence:**

1. **Create Datascript connection**
   ```clojure
   (def conn (create-db))
   ```

2. **Bootstrap from clj-kondo** (static analysis)
   ```clojure
   (bootstrap-from-kondo conn ".")
   ```

3. **Enhance with runtime information**
   ```clojure
   (doseq [v (all-runtime-vars)]
     (enhance-var-with-runtime-info! conn v))
   ```

4. **Install nREPL middleware**
   ```clojure
   (install-nrepl-middleware! conn)
   ```

5. **Middleware auto-captures:**
   - Every eval operation
   - Var redefinitions
   - Namespace reloads
   - File loads

---

## nREPL Middleware Implementation

### Middleware Structure

```clojure
(ns clj-surveyor.nrepl
  (:require [nrepl.middleware :as middleware]
            [nrepl.middleware.print :as print]
            [nrepl.transport :as transport]
            [clj-surveyor.core :as core]
            [datascript.core :as d]))

(def db-conn (atom nil))

(defn wrap-surveyor
  "nREPL middleware that intercepts eval operations."
  [handler]
  (fn [{:keys [op code ns] :as msg}]
    ;; Before eval: capture state
    (when (and (= op "eval") @db-conn)
      (capture-pre-eval-state! @db-conn ns code))
    
    ;; Execute the actual eval
    (let [response (handler msg)]
      
      ;; After eval: detect changes
      (when (and (= op "eval") @db-conn)
        (capture-post-eval-changes! @db-conn ns code))
      
      response)))

(defn capture-pre-eval-state!
  "Snapshot relevant state before eval."
  [conn ns-name code]
  ;; Record that eval is about to happen
  (let [event {:event/id (str "pre-eval-" (System/currentTimeMillis))
               :event/type :eval-started
               :event/timestamp (java.util.Date.)
               :event/source :repl-eval
               :event/namespace ns-name
               :event/code code}]
    (d/transact! conn [event])))

(defn capture-post-eval-changes!
  "Detect and record changes after eval."
  [conn ns-name code]
  (try
    ;; Parse code to find defined vars
    (let [forms (read-string (str "[" code "]"))
          defined-vars (extract-defined-vars forms ns-name)]
      
      ;; Record var changes
      (doseq [var-fqn defined-vars]
        (when-let [v (resolve var-fqn)]
          (record-var-change! conn var-fqn (java.util.Date.) :repl-eval))))
    (catch Exception e
      ;; Log but don't break the REPL
      (println "clj-surveyor: Error capturing changes:" (.getMessage e)))))

(defn extract-defined-vars
  "Extract var symbols from code forms."
  [forms ns-name]
  (keep (fn [form]
          (when (and (seq? form)
                     (contains? #{'def 'defn 'defmacro 'defonce} (first form))
                     (symbol? (second form)))
            (symbol (str ns-name) (str (second form)))))
        forms))

(middleware/set-descriptor!
  #'wrap-surveyor
  {:requires #{}
   :expects #{"eval"}
   :handles {}})
```

### Starting nREPL with Middleware

```clojure
(ns clj-surveyor.nrepl
  (:require [nrepl.server :as nrepl]
            [clj-surveyor.schema :as schema]))

(defn start-server
  "Start nREPL server with clj-surveyor middleware."
  [{:keys [port bind]
    :or {port 7888
         bind "127.0.0.1"}}]
  
  ;; Initialize database
  (reset! db-conn (schema/create-db))
  
  ;; Bootstrap from current runtime
  (bootstrap-initial-state! @db-conn)
  
  ;; Start nREPL with our middleware
  (nrepl/start-server
    :port port
    :bind bind
    :middleware [#'wrap-surveyor]
    :handler (nrepl/default-handler #'wrap-surveyor))
  
  (println (str "clj-surveyor nREPL server started on port " port))
  (println "Entity graph tracking active"))

(defn inject-middleware!
  "Add surveyor middleware to already-running nREPL."
  []
  (reset! db-conn (schema/create-db))
  (bootstrap-initial-state! @db-conn)
  
  ;; Note: This requires nREPL server to support dynamic middleware
  ;; Might need restart in practice
  (println "clj-surveyor middleware injected")
  (println "Note: May require REPL restart to take full effect"))

(defn bootstrap-initial-state!
  "Initial scan of runtime to populate entity graph."
  [conn]
  (println "Scanning runtime for vars...")
  (let [all-vars (all-runtime-vars)
        timestamp (java.util.Date.)]
    
    ;; Create var entities
    (doseq [v all-vars]
      (d/transact! conn [(var->entity v timestamp)]))
    
    (println (str "Indexed " (count all-vars) " vars"))
    
    ;; Build dependency graph
    (println "Building dependency graph...")
    (build-dependency-graph! conn all-vars)
    
    (println "clj-surveyor ready!")))
```

### Message Protocol Extensions (Optional Future)

```clojure
;; Custom nREPL ops for querying

(defn handle-surveyor-query
  "Handle custom 'surveyor/query' op."
  [{:keys [query transport] :as msg}]
  (let [results (execute-query @db-conn query)]
    (transport/send transport
      (response-for msg
        :value (pr-str results)
        :status :done))))

(defn handle-surveyor-dependents
  "Handle custom 'surveyor/dependents' op."
  [{:keys [var transport] :as msg}]
  (let [var-fqn (symbol var)
        deps (find-dependents @db-conn var-fqn)]
    (transport/send transport
      (response-for msg
        :value (pr-str deps)
        :status :done))))

;; Register ops
(middleware/set-descriptor!
  #'wrap-surveyor
  {:requires #{}
   :expects #{"eval"}
   :handles {"surveyor/query" #'handle-surveyor-query
             "surveyor/dependents" #'handle-surveyor-dependents}})
```

---

## Integration with Common Editors

### CIDER (Emacs)

Users can add to their profiles:

```clojure
;; ~/.lein/profiles.clj or ~/.clojure/deps.edn
{:user
 {:dependencies [[clj-surveyor "0.1.0"]]
  :nrepl-middleware [clj-surveyor.nrepl/wrap-surveyor]}}
```

Or start manually:
```emacs-lisp
;; In Emacs
M-x cider-jack-in

;; Then in REPL:
(require '[clj-surveyor.nrepl :as surveyor])
(surveyor/inject-middleware!)
```

### Calva (VSCode)

```clojure
;; .calva/config.edn
{:middleware [clj-surveyor.nrepl/wrap-surveyor]}
```

### Cursive (IntelliJ)

Add to project nREPL configuration or user profile.

---

## Bootstrap Sequence (Phase 0 with nREPL)

**Recommended startup sequence:**

1. **Create Datascript connection**
   ```clojure
   (def conn (create-db))
   ```

2. **Bootstrap from clj-kondo** (static analysis)
   ```clojure
   (bootstrap-from-kondo conn ".")
   ```

3. **Enhance with runtime information**
   ```clojure
   (doseq [v (all-runtime-vars)]
     (enhance-var-with-runtime-info! conn v))
   ```

4. **Install watches for live updates**
   ```clojure
   (watch-all-vars conn)
   ```

5. **Start background pollers**
   ```clojure
   (start-namespace-poller conn 5000)  ; Poll every 5s
   ```

---

## Next Steps

- [ ] Implement core collection functions
- [ ] Test with real Clojure projects
- [ ] Measure performance overhead
- [ ] Add file watching for persistence detection
- [ ] Implement cascade analysis queries
- [ ] Build UI layer on top of entity graph

---

*This implementation guide will evolve as we discover practical details during development.*
