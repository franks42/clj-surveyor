# clj-surveyor Design Document

**Version**: 1.0  
**Date**: October 29, 2025  
**Status**: Initial Design

## Executive Summary

`clj-surveyor` is a comprehensive library for extracting, analyzing, and understanding the structure and behavior of running Clojure codebases. Building upon the foundations of `clj-info` (documentation and introspection) and `clj-ns-browser` (GUI-based exploration), clj-surveyor aims to provide both programmatic APIs and modern web-based interfaces for deep code analysis and comprehension.

**Core Philosophy**: clj-surveyor addresses the fundamental paradox of Clojure development - while the language emphasizes immutability and managed state changes, the development process itself is highly mutable with vars being redefined, namespaces reloaded, and dependencies changed constantly. This runtime mutability is often the source of bugs and surprises that are invisible to traditional file-based analysis tools.

## Vision

To create the definitive toolkit for understanding Clojure codebases at runtime, enabling developers to:
- Navigate complex codebases with confidence
- Understand dependencies, relationships, and architectural patterns
- Identify performance bottlenecks and optimization opportunities  
- Facilitate knowledge transfer and onboarding
- Support modern development workflows with web-based UIs

## Core Principles

1. **Address the Clojure Development Paradox**: While Clojure promotes immutability, development itself is highly mutable (vars redefined, namespaces reloaded, dependencies changed). clj-surveyor makes this invisible mutability visible and trackable.

2. **Runtime-First**: Focus on analyzing live, running systems rather than just static code, because the REPL-driven development model makes runtime the source of truth.

3. **Entity-Centric Analysis**: Treat all code elements (vars, namespaces, dependencies) as entities with relationships, history, and metadata that can be queried and analyzed.

4. **FQN-Based Dependencies**: Focus on actual fully qualified name usage rather than require statements, as requires are convenience mechanisms while FQN usage represents true dependencies.

5. **Development History Tracking**: Capture and make queryable the evolution of code during development sessions to help debug the "surprises" that emerge from runtime mutations.

6. **Multi-Modal**: Support both programmatic APIs and interactive UIs for different analysis needs.

7. **Cross-Platform**: Work seamlessly in JVM Clojure, Babashka, and ClojureScript environments.

8. **Performance-Aware**: Minimal impact on the systems being analyzed while providing maximum insight.

## Use Cases and Requirements

### Based on clj-info (v0.6.0) Capabilities

#### Current Functionality
- **Enhanced Documentation**: Rich formatting, multiple output formats (text, HTML, Markdown, JSON, EDN)
- **ClojureDocs Integration**: Community examples, see-alsos, and notes
- **Cross-Platform Support**: JVM Clojure and Babashka compatibility
- **Protocol-based Architecture**: Extensible documentation rendering
- **Java Interop Analysis**: Java class documentation and javadoc links

#### Extension Opportunities
- Protocol and multimethod introspection
- Type hierarchy visualization  
- Dependency analysis and mapping
- Performance profiling integration
- Static analysis integration (clj-kondo, eastwood)

### Based on clj-ns-browser (v2.0.0) Capabilities

#### Current Functionality
- **Namespace Exploration**: Browse loaded/unloaded namespaces
- **Symbol Analysis**: Functions, macros, protocols, multimethods, Java classes
- **FQN-Focused Interface**: Search and display emphasizes fully qualified names
- **Flexible Information Display**: Doc, source, examples, comments, see-alsos, values
- **Real-time Updates**: Live reflection of namespace and var changes
- **Filtering and Search**: Regex-based filtering with type-specific views (already FQN-aware)
- **Tracing Integration**: Function tracing with tools.trace
- **Visual Organization**: GUI-based browsing with multiple panels

#### clj-ns-browser's FQN Philosophy (Validates Our Insight!)
clj-ns-browser already demonstrates the power of FQN-focused analysis:
- **Search by FQN**: Primary search interface uses fully qualified names
- **Display FQNs**: Shows vars with their complete namespace context
- **Navigation by FQN**: Jump to definitions using full qualification
- **Runtime Focus**: Analyzes what's actually loaded, not just file contents

This validates that **FQN-centric analysis is proven and user-friendly** - clj-ns-browser's success shows developers appreciate this approach!

#### Extension Opportunities: Building on FQN Success
- **Web-based modern UI** to replace Swing (keeping FQN-focused design)
- **Enhanced FQN search** with entity relationships and usage patterns
- **Visual FQN dependency graphs** - show how FQNs actually connect
- **FQN usage analysis** - which FQNs are used where and how often
- **Performance monitoring** per FQN (not just namespace level)
- **Collaborative FQN exploration** for team usage

#### clj-ns-browser → clj-surveyor Evolution

| **clj-ns-browser Strength** | **clj-surveyor Enhancement** |
|------------------------------|------------------------------|
| FQN-focused search & display | **+ FQN dependency tracking** |
| Runtime var introspection | **+ Historical FQN usage patterns** |
| Live namespace updates | **+ Entity change notifications** |
| GUI browsing interface | **+ Web-based collaborative UI** |
| Symbol information display | **+ Cross-reference analysis** |

**Key Innovation**: clj-surveyor adds the **dependency layer** that clj-ns-browser was missing - tracking how FQNs actually relate and depend on each other.

### New Use Cases from Future Features Analysis

#### Enhanced Introspection
1. **Protocol Analysis** (`protocol-info`)
   - Show all implementations of a protocol
   - Method signatures and inheritance chains
   - Usage patterns and dispatch examples

2. **Multimethod Analysis** (`multimethod-info`) 
   - Dispatch function analysis
   - Method implementations and hierarchies
   - Performance characteristics

3. **Type Hierarchy Analysis** (`hierarchy-info`)
   - Complete type ancestry chains
   - Interface implementations
   - Multimethod dispatch implications

#### Dependency and Usage Analysis
1. **Dependency Mapping** (`depends-on`, `used-by`)
   - Static dependency analysis
   - Runtime dependency tracking
   - Circular dependency detection

2. **Usage Pattern Analysis**
   - Function call frequency
   - Performance hotspots
   - Memory usage patterns

#### Developer Productivity
1. **Smart Search and Discovery**
   - Fuzzy search across documentation
   - Similar function suggestions
   - Alternative implementation patterns

2. **Code Quality Analysis**
   - Anti-pattern detection
   - Style guide compliance
   - Idiomatic suggestions
   - Security vulnerability scanning

#### Learning and Education
1. **Interactive Documentation**
   - Live examples in REPL
   - Step-by-step tutorials
   - Learning path recommendations

2. **Visual Understanding**
   - Call graph visualization
   - Data flow diagrams
   - Architecture overviews

## Entity-Centric Analysis Framework

A core principle of clj-surveyor is **multi-perspective analysis** - understanding any entity in a codebase from multiple viewpoints. Each entity type provides different analytical lenses, and the system should seamlessly navigate between these perspectives.

### Analysis Perspectives

> **Note on Clojure's Dynamic Nature**: Unlike file-centric languages, Clojure's REPL-driven development, dynamic loading, and namespace flexibility make **runtime state** more important than file organization. A namespace can be defined across multiple files, vars can be dynamically created, and code can be evaluated from anywhere. Therefore, clj-surveyor prioritizes **namespace-centric** and **FQN-var-centric** views over file-based analysis.

#### 1. Runtime System Analysis
**View**: The live, running Clojure system as the primary source of truth
- **Collection of Namespaces**: All loaded namespaces, their runtime dependencies, and dynamic relationships
- **Collection of FQN-vars**: Complete runtime symbol table with current values and metadata
- **Protocol/Type System**: All protocols, types, and their actual runtime implementations
- **Performance Profile**: Real-time hotspots, memory usage, call patterns
- **Dynamic State**: REPL history, dynamically created vars, runtime modifications

```clojure
(analyze-runtime-system {:include-repl-state true
                        :track-dynamic-changes true
                        :perspectives [:namespaces :vars :protocols :performance]})
;; => {:namespaces {live-ns-graph ...}        ; From (all-ns) 
;;     :vars {runtime-symbol-table ...}       ; From actual loaded vars
;;     :protocols {actual-implementations ...} ; Runtime protocol extensions
;;     :performance {live-profile-data ...}    ; Real performance data
;;     :dynamic-state {repl-history ...}}      ; REPL and dynamic changes
```

#### 2. Namespace-Level Analysis (Primary Focus)
**View**: Namespace as the fundamental unit of code organization in Clojure
- **Runtime Namespace State**: Current loaded state, not just file contents
- **Defined Vars**: All vars currently in the namespace (including REPL-defined)
- **Dependencies**: Runtime requires, imports, and dynamic dependencies  
- **Dependents**: What namespaces currently depend on this one
- **API Surface**: Actual public interface as seen by runtime
- **Dynamic History**: REPL modifications, reloads, runtime changes
- **Cross-File Definitions**: Vars defined in multiple files or REPL

```clojure
(analyze-namespace 'my.app.core)
;; => {:name 'my.app.core
;;     :runtime-state :loaded  ; :loaded, :unloaded, :partially-loaded
;;     :source-files ["src/my/app/core.clj" "dev/patches.clj"]  ; Multiple files possible
;;     :defined-vars [{:fqn 'my.app.core/process-data :type :function 
;;                     :defined-in :file :source "src/my/app/core.clj"}
;;                    {:fqn 'my.app.core/debug-mode :type :var
;;                     :defined-in :repl :repl-session 42}]
;;     :dependencies [:clojure.string :clojure.set :my.app.utils]
;;     :dependents [:my.app.handlers :my.app.routes]
;;     :api-surface {:public-vars [...] :protocols [...]}
;;     :dynamic-history {:reloads 3 :last-reload "2025-10-29T14:30:00"
;;                      :repl-definitions [:debug-mode :temp-fn]}}
```

#### 3. FQN-Var Analysis (Primary Focus)
**View**: Individual vars as the atomic units of functionality
- **Runtime Value**: Current actual value, not just source definition
- **Definition Source**: Where defined (file, REPL, dynamically)
- **Usage Tracking**: Real runtime usage patterns and call sites  
- **Dynamic Changes**: History of redefinitions and modifications
- **Type and Protocol Information**: Actual runtime type relationships
- **Performance Data**: Real call frequencies, timing, memory usage

```clojure
(analyze-fqn-var 'my.app.core/process-data)
;; => {:fqn 'my.app.core/process-data
;;     :current-value #function[my.app.core/process-data]
;;     :definition-source {:type :file :path "src/my/app/core.clj" :line 25}
;;     :redefinitions [{:timestamp "2025-10-29T14:25:00" :source :repl}
;;                     {:timestamp "2025-10-29T14:28:00" :source :file}]
;;     :runtime-usage {:call-sites ['my.app.handlers/handle-request 
;;                                  'my.app.routes/process-route]
;;                     :call-count 1547 :last-called "2025-10-29T14:30:15"}
;;     :type-info {:arglists '([data] [data options]) 
;;                 :protocols #{Processable DataHandler}
;;                 :return-type 'clojure.lang.IPersistentMap}
;;     :dependencies ['clojure.string/trim 'my.app.utils/validate]
;;     :performance {:avg-time-ms 2.3 :memory-mb 0.5 :gc-pressure :low}}
```

#### 4. File-Level Analysis (Secondary)
**View**: Files as containers, less important in runtime-driven analysis
> **Note**: File analysis is secondary since Clojure code organization is namespace-driven. A namespace can span multiple files, and a file can define multiple namespaces. File analysis is mainly useful for build tools, editors, and understanding code organization patterns.

- **Top-Level Forms**: Sequence of forms in evaluation order
- **Namespace Contributions**: What namespaces this file contributes to
- **Build Relationships**: Dependencies for compilation and loading
- **Change Impact**: What gets invalidated when this file changes

```clojure
(analyze-file "src/my/app/core.clj")
;; => {:path "src/my/app/core.clj"
;;     :namespace-contributions {'my.app.core #{process-data validate helper}
;;                              'my.app.core.internals #{internal-fn}}
;;     :top-level-forms [{:type :ns :form '(ns my.app.core ...) :line 1}
;;                      {:type :defn :form '(defn process-data ...) :line 10}]
;;     :build-info {:load-order 15 :compilation-deps [...]}
;;     :change-impact {:affected-namespaces ['my.app.core]
;;                    :requires-restart false}}
```

#### 5. Expression/Form Analysis (Least Important)
**View**: Individual forms mainly for editor tooling and static analysis
> **Note**: In Clojure's dynamic environment, the runtime result matters more than the original form. This level is primarily useful for editors, refactoring tools, and understanding code structure patterns.

- **Form Structure**: AST-like analysis of code structure
- **Static Dependencies**: Dependencies visible in source (may differ from runtime)
- **Complexity Metrics**: Static analysis of complexity
- **Editor Support**: Information for IDEs and refactoring tools

```clojure
```clojure
(analyze-form '(defn process-data [data] (-> data validate transform)))
;; => {:type :defn
;;     :static-analysis {:defines ['process-data]
;;                      :static-deps ['validate 'transform]
;;                      :complexity {:cyclomatic 2}}
;;     :editor-info {:parameters [data] :return-path [...]}
;;     :refactoring-hints {:extract-function-candidates [...]}}
```

### Runtime-First Philosophy

#### Why Runtime Analysis Matters in Clojure

**REPL-Driven Development**: 
- Code is often developed interactively at the REPL
- Functions get redefined multiple times during development
- The "final" version may never be saved to a file
- Runtime state reflects the current working version

**Dynamic Code Loading**:
```clojure
;; Code can be evaluated from anywhere
(eval '(defn my-dynamic-fn [x] (* x 2)))

;; Namespaces can be modified at runtime  
(in-ns 'my.temp.ns)
(def temp-var "only exists in this session")

;; Protocol extensions can be added dynamically
(extend-protocol MyProtocol
  String (my-method [s] ...))
```

**Flexible Namespace Management**:
```clojure
;; Single file can contribute to multiple namespaces
(ns my.app.core)
(defn core-fn [])

(ns my.app.utils)  ; Same file, different namespace
(defn util-fn [])

;; Multiple files can contribute to one namespace
(ns my.app.core)  ; in core.clj
(defn main-fn [])

(ns my.app.core)  ; in patches.clj  
(defn patch-fn [])  ; Added to same namespace
```

#### Implications for Analysis

1. **Source of Truth**: Runtime state (loaded namespaces, actual vars) is more reliable than file contents
2. **Dynamic Dependencies**: `(all-ns)` and `ns-interns` reveal actual relationships
3. **Evolution Tracking**: Need to track how code changes over time through REPL sessions
4. **Performance Reality**: Only runtime analysis shows actual performance characteristics
5. **Protocol Extensions**: Runtime extensions may not be visible in source files

#### Analysis Strategy

**Primary Sources** (runtime-first):
```clojure
;; Get actual loaded namespaces
(all-ns)                           ; What's actually loaded
(ns-interns 'my.ns)               ; What vars actually exist
(ns-refers 'my.ns)                ; What's actually available
(ns-aliases 'my.ns)               ; Actual aliases in use

;; Get actual dependencies (not just file requires)
(dependencies-of-loaded-ns 'my.ns) ; What it actually uses at runtime
```

**Secondary Sources** (file-based, for tooling):
```clojure
;; Files for editor support, build tools, etc.
(analyze-file "src/my/app/core.clj") ; Static structure
(file-to-namespace-mapping)         ; Build relationships
```

### Cross-Entity Navigation
```



### Cross-Entity Navigation (Runtime-First)

The system should support seamless navigation between runtime entities, treating the live system as the primary source of truth:

```clojure
;; Start with runtime system, explore loaded namespaces
(-> (analyze-runtime-system)
    :namespaces
    (filter #(str/starts-with? (str %) "my.app"))
    (map analyze-namespace))

;; Drill down from namespace to actual loaded vars
(-> (analyze-namespace 'my.app.core)
    :defined-vars                    ; Runtime vars, not file-based
    (filter #(= (:type %) :function))
    (map :fqn)
    (map analyze-fqn-var))

;; Trace runtime dependencies (more accurate than file analysis)
(-> (analyze-fqn-var 'my.app.core/process-data)
    :runtime-usage                   ; Actual calls observed at runtime
    :call-sites
    (map :calling-namespace)
    distinct
    (map analyze-namespace))

;; Navigate protocol relationships (runtime extensions)
(-> (analyze-runtime-system)
    :protocols
    (get 'my.core/Processable)
    :actual-implementations          ; Runtime extensions, not just source
    (map analyze-fqn-var))

;; Follow dynamic redefinition history
(-> (analyze-fqn-var 'my.debug/temp-fn)
    :redefinition-chain             ; How this var evolved in REPL
    (map :definition-source))
```

### The True Nature of Dependencies: A Critical Insight

#### Requires are Convenience, Not Dependencies

A fundamental insight that reshapes how clj-surveyor should approach dependency analysis:

**`require` statements are purely a convenience mechanism** for developers, not fundamental dependency declarations. They exist only to:

1. **Create aliases** for fully qualified names (FQNs)
2. **Make code more readable** by avoiding repetitive namespace prefixes  
3. **Import symbols** into the current namespace scope

**The real dependencies** are the actual **fully qualified var references** in the code. If all vars were written as FQNs, no `require` statements would be needed at all:

```clojure
;; Instead of this:
(ns my.app 
  (:require [clojure.string :as str]
            [my.utils :refer [helper-fn]]))

(defn process [text]
  (str/upper-case (helper-fn text)))

;; We could write this (no requires needed):
(ns my.app)

(defn process [text]
  (clojure.string/upper-case (my.utils/helper-fn text)))
```

#### Implications for clj-surveyor Architecture

This insight fundamentally changes how dependency analysis should work:

- **Dependency analysis** should focus on **FQN usage**, not require statements
- **Unused requires** detection becomes trivial: any require not reflected in FQN usage
- **Dynamic dependencies** can be detected by analyzing actual var references at runtime
- **True dependency graphs** emerge from var usage patterns, not namespace declarations
- **Var-level granularity** provides much richer dependency information than namespace-level

#### Validation from clj-ns-browser Success

**clj-ns-browser already demonstrates this approach works!** Its FQN-focused design proves that:

1. **Users prefer FQN-based interfaces** - they want to see and search by full names
2. **FQN navigation is intuitive** - jump to definitions using complete qualification
3. **Runtime FQN analysis is valuable** - analyze what's loaded, not file contents  
4. **FQN search is powerful** - primary interface uses fully qualified names

clj-surveyor can build on this proven foundation while adding the missing piece: **FQN-based dependency analysis**

#### Entity Relationships Based on Usage

```clojure
;; Traditional approach: namespace -> namespace dependencies
{:ns/dependencies ["clojure.string" "my.utils"]}

;; clj-surveyor approach: var -> var dependencies  
{:var/fqn 'my.app/process
 :var/uses ['clojure.string/upper-case 'my.utils/helper-fn]
 :var/direct-deps 2
 :var/transitive-deps 15}
```

This enables much more sophisticated analysis:
- Which specific vars are actually used from a dependency?
- What's the true impact of removing a var?
- Which vars could be extracted to separate libraries?
- How tightly coupled are different parts of the codebase?

### The Clojure Development Paradox: Immutable Language, Mutable Runtime

#### The Fundamental Contradiction

**Clojure embodies a profound paradox**: It's a functional programming language with strong emphasis on **immutability** and **managed state changes**, yet during development and building up of an application's runtime, **it is extremely mutable**. Following these state changes is not trivial at all and is probably the **source of many bugs and "surprises"**.

This creates a unique challenge:

```clojure
;; The code advocates immutability...
(def users (atom #{}))
(swap! users conj new-user)  ; Managed, controlled state change

;; ...but the development process is highly mutable
(def process-user [user] ...)     ; Define function
(def process-user [user] ...)     ; Redefine with different logic  
(ns-unalias *ns* 'utils)          ; Change namespace mappings
(require 'new.dependency :reload) ; Alter loaded code
```

**The Result**: While application data flows are predictable and immutable, the **development environment itself** is in constant flux - vars are redefined, namespaces are reloaded, dependencies change, and the runtime state evolves continuously.

#### Why This Matters for clj-surveyor

This paradox explains why **runtime-first analysis** is not just preferable but **essential** for Clojure development tooling:

1. **File-based analysis captures the intention**, but **runtime analysis captures the reality**
2. **Static analysis shows what code should do**, **runtime analysis shows what code actually does**
3. **The REPL-driven development model makes runtime the source of truth**, not files
4. **Bugs and surprises emerge from the gap** between static code and runtime state

#### Addressing the Development Mutability Problem

clj-surveyor's entity-centric approach directly addresses this paradox by:

**Tracking Runtime Mutations as First-Class Entities:**
```clojure
;; Each redefinition becomes a tracked entity with history
{:entity/fqn     'my.app/process-user
 :entity/version 3
 :entity/redefined-at #inst "2024-10-29T15:30:00"
 :entity/previous-versions [v1-entity v2-entity]
 :redefinition/source :repl-eval
 :redefinition/reason "Added error handling"}
```

**Making Development History Visible:**
- **Who changed what when** during development sessions
- **Why vars were redefined** (if captured via tooling)
- **What dependencies were added/removed** dynamically
- **Which namespace reloads affected** which parts of the system

**Bridging the Immutable-Mutable Gap:**
- **Immutable entity records** of mutable development actions
- **Persistent history** of ephemeral REPL interactions  
- **Queryable timeline** of development state changes
- **Diff analysis** between file state and runtime state

#### Example: Debugging the "Surprise"

```clojure
;; Developer confusion: "Why is my function behaving differently?"

;; clj-surveyor can show the development history:
(entity-history 'my.app/process-user)
;; => 
;; Version 1: Defined from file at 10:30 AM
;; Version 2: Redefined in REPL at 11:15 AM (added logging)  
;; Version 3: Redefined in REPL at 2:30 PM (removed logging, added validation)
;; Current runtime differs from file by 23 lines

;; Find the source of surprise:
(runtime-vs-file-diff 'my.app/process-user)
;; => Shows exactly what changed and when
```

This transforms the **"mutable development mystery"** into **"visible development evolution"**.

### Dynamic State Management: The Moving Target Problem

#### The Challenge of Ever-Changing Runtime State

In Clojure, any `eval` can fundamentally alter the runtime state:

```clojure
;; Before: clean dependency graph
(analyze-namespace 'my.app.core)
;; => {:dependencies [:clojure.string :my.utils]}

;; Developer evaluates new code at REPL
(eval '(in-ns 'my.app.core))
(eval '(require '[clojure.java.io :as io]))
(eval '(defn new-fn [path] (io/file path)))
(eval '(alter-var-root #'existing-fn 
         (constantly (fn [x] (new-fn (str x ".tmp"))))))

;; After: completely different dependency graph and execution behavior
(analyze-namespace 'my.app.core)  
;; => {:dependencies [:clojure.string :my.utils :clojure.java.io]
;;     :new-vars [new-fn]
;;     :redefined-vars [existing-fn]}
```

#### Implications for Analysis

**Snapshot Nature**: Any analysis is a point-in-time snapshot that becomes stale immediately:
```clojure
(def analysis-t0 (analyze-namespace 'my.ns))    ; State at time T0
;; ... developer works at REPL for 10 minutes ...
(def analysis-t1 (analyze-namespace 'my.ns))    ; State at time T1
;; analysis-t0 and analysis-t1 may be completely different
```

**Stale Data Problem**: Analysis results have a "freshness" problem:
- Dependency graphs may be outdated
- Function implementations may have changed
- New vars may exist that weren't in the original analysis
- Performance characteristics may be completely different

#### Dynamic Update Strategies

##### 1. Change Detection and Invalidation
```clojure
(defprotocol ChangeAware
  (generation [entity] "Return generation/version number")
  (invalidate-on-change [entity] "Mark as needing re-analysis"))

;; Track namespace changes
(def ns-generations (atom {}))

(defn namespace-changed? [ns-sym]
  (let [current-gen (generation (find-ns ns-sym))
        recorded-gen (get @ns-generations ns-sym)]
    (not= current-gen recorded-gen)))

;; Automatic invalidation on eval
(defn hook-eval-monitoring []
  (add-watch #'*ns* ::analysis-invalidator
    (fn [key ref old-state new-state]
      (invalidate-analysis-cache new-state))))
```

##### 2. Incremental Analysis Updates
```clojure
;; Instead of full re-analysis, update incrementally
(defn incremental-update [old-analysis new-vars removed-vars changed-vars]
  (-> old-analysis
      (update :vars #(apply dissoc % removed-vars))
      (update :vars #(merge % (analysis-for-vars new-vars)))
      (update :vars #(merge % (re-analyze-vars changed-vars)))
      (recalculate-dependencies)
      (update-generation)))

;; Example usage
(def updated-analysis 
  (incremental-update previous-analysis 
                     #{new-fn helper-fn}     ; new vars
                     #{old-deprecated-fn}    ; removed vars  
                     #{existing-fn}))        ; changed vars
```

##### 3. Lazy Analysis with Freshness Tracking
```clojure
(defn analyze-with-freshness [entity max-age-ms]
  (let [cached (get-cached-analysis entity)
        age (- (System/currentTimeMillis) (:timestamp cached))]
    (if (or (nil? cached) (> age max-age-ms))
      (do
        (log/debug "Re-analyzing" entity "due to staleness")
        (cache-analysis entity (fresh-analysis entity)))
      (do
        (log/debug "Using cached analysis for" entity)
        cached))))

;; Usage with different freshness requirements
(analyze-namespace 'my.ns {:max-age-ms 5000})    ; 5 second freshness
(analyze-fqn-var 'my/fn {:max-age-ms 1000})      ; 1 second freshness
```

##### 4. Real-time Change Monitoring
```clojure
;; Monitor namespace changes in real-time
(defn start-runtime-monitoring []
  (let [watch-key ::clj-surveyor-monitor]
    ;; Watch for var changes
    (doseq [ns-sym (map ns-name (all-ns))]
      (add-namespace-watch ns-sym watch-key
        (fn [ns-sym event var-sym old-val new-val]
          (case event
            :var-added    (handle-var-addition ns-sym var-sym new-val)
            :var-removed  (handle-var-removal ns-sym var-sym)
            :var-changed  (handle-var-change ns-sym var-sym old-val new-val)))))
    
    ;; Watch for namespace changes  
    (add-ns-creation-watch watch-key handle-namespace-creation)
    (add-ns-removal-watch watch-key handle-namespace-removal)))

;; Event handlers update analysis incrementally
(defn handle-var-change [ns-sym var-sym old-val new-val]
  (log/info "Var changed:" ns-sym "/" var-sym)
  (invalidate-analysis-cache ns-sym)
  (invalidate-dependent-analyses ns-sym var-sym)
  (schedule-incremental-update ns-sym var-sym))
```

##### 5. Analysis Confidence Levels
```clojure
;; Include confidence/freshness metadata in all analysis results
(defn enhanced-analysis [entity]
  (let [analysis (base-analysis entity)
        confidence (calculate-confidence-level analysis)]
    (assoc analysis 
           :meta {:timestamp (System/currentTimeMillis)
                 :confidence confidence    ; :high, :medium, :low, :stale
                 :staleness-indicators (detect-staleness-indicators analysis)
                 :last-change-detected (:last-change @runtime-change-tracker)})))

;; Example result with confidence metadata
(analyze-namespace 'my.app.core)
;; => {:name 'my.app.core
;;     :dependencies [...]
;;     :defined-vars [...]
;;     :meta {:timestamp 1698600000000
;;           :confidence :medium          ; Not :high due to recent REPL activity
;;           :staleness-indicators [:recent-eval :namespace-reload]
;;           :last-change-detected 1698599995000}}  ; 5 seconds ago
```

#### Best Practices for Handling Dynamic State

##### 1. Embrace Approximate Accuracy
- Accept that analysis will always be somewhat out-of-sync
- Provide confidence indicators to users
- Design UIs that gracefully handle stale data

##### 2. Multi-Level Caching Strategy
```clojure
;; Different cache lifetimes for different analysis types
(def cache-strategies
  {:namespace-structure  {:ttl-ms 30000}    ; 30 seconds (fairly stable)
   :dependency-graph     {:ttl-ms 10000}    ; 10 seconds (changes with requires)
   :var-definitions      {:ttl-ms 2000}     ; 2 seconds (changes with defn)
   :runtime-values       {:ttl-ms 500}      ; 500ms (changes constantly)
   :performance-data     {:ttl-ms 5000}})   ; 5 seconds (accumulates over time)
```

##### 3. User-Initiated Refresh
```clojure
;; Always provide explicit refresh mechanisms
(defn force-refresh-analysis [entity]
  (clear-cache entity)
  (fresh-analysis entity))

;; Batch refresh for efficiency
(defn refresh-all-analyses []
  (clear-all-caches)
  (parallel-reanalysis (all-entities)))
```

##### 4. Change Broadcasting
```clojure
;; Notify all interested parties when changes occur
(defprotocol ChangeListener
  (on-namespace-change [listener ns-sym change-type])
  (on-var-change [listener ns-sym var-sym change-type])
  (on-dependency-change [listener from-entity to-entity change-type]))

;; Web UI, cached analyses, etc. can all listen for changes
(register-change-listener web-ui-updater)
(register-change-listener cache-invalidator)
(register-change-listener dependency-graph-updater)
```

#### Implementation Considerations

**Performance vs. Accuracy Trade-offs**:
- More frequent updates = higher accuracy, higher overhead
- Longer cache lifetimes = better performance, potentially stale data
- User should be able to configure this trade-off

**Change Detection Granularity**:
- Coarse-grained: Namespace-level change detection (simpler, less accurate)
- Fine-grained: Individual var-level change tracking (complex, more accurate)

**Concurrent Access**:
- Multiple threads may be analyzing while code changes
- Need thread-safe caching and analysis strategies
- Consider copy-on-write for analysis data structures

### Universal Analysis Queries

Each entity should support common analytical questions:

#### "Where is X defined/used?"
```clojure
(where-defined? 'my.app.core/process-data)
(where-used? 'my.app.core/process-data)
(cross-references 'my.app.core/process-data)
```

#### "What are the dependencies/dependents?"
```clojure
(dependencies entity)      ; What does this entity depend on?
(dependents entity)        ; What depends on this entity?
(dependency-chain entity)  ; Full dependency chain
```

#### "What are the relationships?"
```clojure
(isa-relationships entity)        ; Type hierarchy relationships
(protocol-relationships entity)   ; Protocol implementations
(multimethod-relationships entity) ; Multimethod dispatch relationships
```

#### "What can I learn about this entity?"
```clojure
(entity-summary entity)           ; High-level overview
(quality-metrics entity)          ; Code quality assessments  
(performance-profile entity)      ; Runtime performance data
(usage-patterns entity)           ; How is it typically used?
```

### Implementation Strategy

#### 1. Entity Abstraction
```clojure
(defprotocol Analyzable
  "Protocol for entities that can be analyzed"
  (analyze [entity opts] "Return comprehensive analysis of entity")
  (dependencies [entity] "Return entities this depends on")  
  (dependents [entity] "Return entities that depend on this")
  (relationships [entity] "Return type/protocol/hierarchy relationships")
  (usage-patterns [entity] "Return usage pattern analysis"))

;; Implement for each entity type
(extend-protocol Analyzable
  clojure.lang.Namespace ...)
  clojure.lang.Var ...)
  java.io.File ...)  ; for file analysis
  clojure.lang.PersistentList ...) ; for top-level statements
```

#### 2. Cross-Reference Database
- Maintain comprehensive cross-reference database
- Track all relationships between entities
- Support fast lookups and reverse lookups
- Enable graph traversal and analysis

#### 3. Lazy and Incremental Analysis
- Only analyze entities when requested
- Cache results with invalidation on code changes
- Support incremental updates for large codebases
- Background analysis for better interactivity

### Practical Use Cases

#### Codebase Exploration
```clojure
;; "I want to understand this new codebase"
(def codebase-overview (analyze-codebase {:root-path "src/"}))

;; See the high-level structure
(:namespace-graph codebase-overview)  ; How namespaces relate
(:api-surface codebase-overview)      ; Public interface
(:entry-points codebase-overview)     ; Main functions and protocols

;; Find the most important/central pieces
(most-depended-on codebase-overview)  ; What everything else uses
(architectural-layers codebase-overview) ; Identify layering
```

#### Impact Analysis  
```clojure
;; "If I change this function, what breaks?"
(def impact (analyze-fqn-var 'my.core/critical-function))
(:dependents impact)                  ; Direct dependents
(transitive-dependents impact)        ; Everything that could be affected
(change-impact-report impact)         ; Detailed impact analysis

;; "What tests do I need to run?"
(affected-test-namespaces impact)
```

#### Refactoring Support
```clojure
;; "Can I safely move this function to another namespace?"
(def move-analysis (analyze-move 'my.utils/helper-fn 'my.core))
(:breaking-changes move-analysis)     ; What would break
(:suggested-aliases move-analysis)    ; Recommended import aliases

;; "What would happen if I delete this namespace?"
(deletion-impact 'my.deprecated.utils)
```

#### Learning and Documentation
```clojure
;; "How do I use this protocol?"  
(def protocol-guide (analyze-protocol 'my.core/Processable))
(:implementations protocol-guide)     ; All implementations
(:usage-examples protocol-guide)      ; Real usage in codebase
(:best-practices protocol-guide)      ; Common patterns

;; "Show me examples of similar functions"
(similar-functions 'clojure.core/map {:criteria [:arity :pattern :domain]})
```

#### Performance Investigation
```clojure
;; "Why is my application slow?"
(def perf-analysis (analyze-codebase {:include-performance true}))
(:hotspots perf-analysis)            ; CPU bottlenecks  
(:memory-hogs perf-analysis)         ; Memory usage issues
(:call-frequency perf-analysis)      ; Most called functions

;; "What's expensive about this function?"
(analyze-fqn-var 'my.core/expensive-fn {:include-profiling true})
```

#### Code Quality Assessment
```clojure
;; "How good is the code quality?"
(def quality-report (analyze-codebase {:include-quality-metrics true}))
(:complexity-hotspots quality-report)  ; Most complex code
(:documentation-gaps quality-report)   ; Poorly documented areas  
(:anti-patterns quality-report)        ; Code smells and issues

;; "Is this namespace well-designed?"
(quality-metrics (analyze-namespace 'my.questionable.ns))
```

## Architecture

### Core Modules

#### 1. Dynamic State Management (`clj-surveyor.state`)
- Runtime change detection and monitoring
- Cache invalidation and freshness tracking
- Incremental analysis updates
- Concurrent access coordination
- REPL integration and eval monitoring

```clojure
(ns clj-surveyor.state
  "Dynamic runtime state management and change tracking")

;; Change monitoring
(defn start-runtime-monitoring [])               ; Begin tracking runtime changes
(defn stop-runtime-monitoring [])                ; Stop change tracking
(defn register-change-listener [listener])       ; Register for change notifications

;; Cache management with staleness awareness
(defn cached-analysis [entity opts])             ; Get cached analysis with freshness check
(defn invalidate-cache [entity])                 ; Force cache invalidation
(defn cache-confidence-level [entity])           ; Get confidence in cached data

;; Incremental updates
(defn incremental-update [old-analysis changes]) ; Update analysis incrementally
(defn detect-changes [entity])                   ; Detect what changed since last analysis
(defn batch-update-analyses [entities])          ; Efficiently update multiple analyses

;; Concurrent access coordination
(defn with-analysis-lock [entity f])             ; Coordinate concurrent analysis
(defn snapshot-runtime-state [])                 ; Create consistent runtime snapshot
```

#### 2. Entity Storage (`clj-surveyor.db`)
- Datascript-based entity-relationship storage
- Complex datalog queries for relationship traversal
- Time-based queries for historical analysis  
- Reactive queries for real-time updates
- Schema management for evolving data models

```clojure
(ns clj-surveyor.db
  "Entity-relationship storage using Datascript")

;; Database operations
(defn add-entity [entity])                        ; Store entity in database
(defn update-entity [entity-id updates])          ; Update entity attributes
(defn remove-entity [entity-id])                  ; Remove entity from database
(defn add-relationship [from-id to-id rel-type attrs]) ; Add relationship

;; Complex relationship queries
(defn transitive-dependencies [entity-id])        ; Find all transitive dependencies
(defn dependency-paths [from-id to-id])           ; Find all paths between entities
(defn circular-dependencies [scope])              ; Detect circular dependencies
(defn unused-entities [scope criteria])           ; Find unused entities

;; Historical and reactive queries
(defn entity-at-time [entity-id timestamp])       ; Historical state
(defn changes-since [timestamp])                  ; Changes since timestamp
(defn register-query-listener [query callback])   ; Reactive query updates
(defn snapshot-database [])                       ; Create database snapshot
```

#### 3. Data Collection (`clj-surveyor.collect`)
- Runtime introspection APIs
- Namespace and var analysis with dynamic awareness
- Integration with entity storage system
- Performance metrics collection  
- Memory usage tracking

```clojure
(ns clj-surveyor.collect
  "Core data collection and analysis functions")

;; Entity-centric analysis functions
(defn analyze-codebase [opts])                    ; Codebase-level analysis
(defn analyze-namespace [ns-sym])                 ; Namespace-level analysis  
(defn analyze-file [file-path])                   ; File-level analysis
(defn analyze-top-level-statement [form-data])    ; Statement-level analysis
(defn analyze-fqn-var [fqn])                     ; Var-level analysis

;; Cross-entity queries
(defn where-defined? [entity])                    ; Find definition locations
(defn where-used? [entity])                       ; Find usage locations  
(defn dependencies [entity])                      ; What does entity depend on?
(defn dependents [entity])                        ; What depends on entity?
(defn cross-references [entity])                  ; All references to entity

;; Specialized introspection (from clj-info future features)
(defn protocol-info [protocol])                   ; Protocol implementations
(defn multimethod-info [multimethod])             ; Multimethod dispatch analysis
(defn hierarchy-info [type-or-protocol])          ; Type hierarchy relationships

;; Dependency graph analysis  
(defn dependency-graph [root-entity])             ; Build dependency graph
(defn circular-dependencies [scope])              ; Detect circular deps
(defn unused-dependencies [scope])                ; Find unused deps

;; Performance analysis
(defn hotspot-analysis [scope])                   ; Performance bottlenecks
(defn memory-analysis [entity])                   ; Memory usage patterns
```

#### 2. Analysis Engine (`clj-surveyor.analyze`)
- Pattern recognition and analysis
- Code quality assessment
- Performance bottleneck identification
- Learning path generation

```clojure
(ns clj-surveyor.analyze
  "Higher-level analysis and pattern recognition")

;; Code quality
(defn anti-patterns [code-base])
(defn style-compliance [ns-or-fn])
(defn optimization-suggestions [fn-var])

;; Learning assistance
(defn similar-functions [fn-var])
(defn learning-path [concept])
(defn difficulty-rating [fn-var])
```

#### 4. Visualization (`clj-surveyor.viz`)
- Entity-relationship visualizations powered by Datascript queries
- Interactive dependency graphs with historical views  
- Statistical analysis displays from aggregation queries
- ASCII art diagrams for terminal
- HTML/SVG generation for web
- Export capabilities

```clojure
(ns clj-surveyor.viz
  "Visualization and diagram generation integrated with entity storage")

;; Entity-based visualizations
(defn entity-relationship-diagram [query filters])  ; ER diagram from query
(defn dependency-graph [entity-scope options])      ; Visualize dependencies
(defn circular-dependency-view [scope])             ; Highlight circular deps
(defn unused-code-heatmap [criteria])               ; Show unused entities

;; Interactive exploration
(defn entity-explorer [starting-entity])            ; Interactive entity browser
(defn relationship-drill-down [entity-id])          ; Explore relationships  
(defn historical-timeline [entity-id])              ; Show entity evolution
(defn search-entities [search-criteria])            ; Search and filter entities

;; ASCII visualizations (terminal-friendly)
(defn ascii-dependency-graph [entity-query])
(defn ascii-call-tree [fn-entity-id])
(defn ascii-hierarchy-tree [type-entity-id])

;; Web visualizations
(defn html-dependency-graph [entity-query])
(defn interactive-call-graph [fn-entity-id])
(defn architecture-overview [namespace-entities])
```

#### 5. Web Interface (`clj-surveyor.web`)
- Modern web UI to replace clj-ns-browser Swing interface
- **Enhanced FQN-focused experience** building on clj-ns-browser's proven approach
- RESTful API for programmatic access
- Real-time updates via WebSockets and reactive Datascript queries
- **FQN-based dependency visualization** - the missing piece clj-ns-browser couldn't provide
- Collaborative features with shared entity views

```clojure
(ns clj-surveyor.web
  "Web-based interface and API powered by entity storage")

;; Web server
(defn start-server [opts])
(defn stop-server [])

;; Entity-based API endpoints
(defn api-entity [entity-id])                     ; Get entity details
(defn api-entity-relationships [entity-id type])  ; Get relationships
(defn api-query [datalog-query params])           ; Execute datalog query
(defn api-search [search-criteria])               ; Search entities
(defn api-historical [entity-id timestamp])       ; Historical entity state

;; Real-time updates
(defn websocket-entity-updates [entity-filter])   ; Subscribe to entity changes
(defn reactive-query-stream [query])              ; Stream query results
```

#### 6. Integration (`clj-surveyor.integrations`)
- Editor integrations (LSP, nREPL middleware) with entity queries
- Build tool plugins with entity analysis
- CI/CD integrations with entity change detection  
- External tool connectivity via entity API
- Migration tools from existing analysis systems

```clojure
(ns clj-surveyor.integrations
  "Integration with external tools and editors")

;; nREPL middleware
(defn surveyor-middleware [])

;; LSP integration
(defn hover-info [symbol])
(defn completion-suggestions [prefix])

;; Build tools
(defn leiningen-plugin [])
(defn deps-edn-integration [])
```

### Entity-Relationship Storage Strategy

#### The Graph Database Challenge

clj-surveyor deals with a complex web of entities and relationships:

**Entity Types**: Namespaces, Vars, Files, Protocols, Multimethods, Types, etc.  
**Relationship Types**: depends-on, defines, implements, extends, calls, etc.  
**Attributes**: Each entity has numerous attributes that change over time

**Example Complexity**:
```clojure
;; A single namespace creates a web of relationships
'my.app.core
├─ :defines → ['my.app.core/process-data 'my.app.core/validate]
├─ :depends-on → ['clojure.string 'my.utils 'clojure.java.io]  
├─ :depended-on-by → ['my.app.handlers 'my.app.routes]
├─ :implements → ['my.protocols/Processable]
├─ :file-source → "src/my/app/core.clj"
└─ :performance → {:startup-time 45ms :memory 1.2mb}

'my.app.core/process-data
├─ :defined-in → 'my.app.core  
├─ :calls → ['clojure.string/trim 'my.utils/validate]
├─ :called-by → ['my.app.handlers/handle-request]
├─ :type → :function
├─ :arglists → '([data] [data opts])
├─ :current-value → #function[...]
└─ :performance → {:call-count 1547 :avg-time 2.3ms}
```

This creates a highly interconnected graph where queries like "show me all vars that depend on vars defined in namespaces that depend on clojure.java.io" become complex to express and execute efficiently.

#### Storage Strategy Options

##### Option 1: Datascript (Recommended)

**Advantages**:
- **Datalog Queries**: Perfect for complex relationship traversals
- **Immutable Database**: Natural fit for Clojure's philosophy
- **Time-Based Queries**: Can query historical states
- **Reactive Queries**: Automatic updates when data changes
- **ClojureScript Compatible**: Can run in web UI
- **Small Footprint**: In-memory, lightweight

```clojure
(ns clj-surveyor.db
  (:require [datascript.core :as d]))

;; Schema definition
(def schema
  {:entity/id            {:db/unique :db.unique/identity}
   :entity/type          {:db/index true}
   :entity/name          {:db/index true}
   :namespace/name       {:db/unique :db.unique/identity}
   :var/fqn             {:db/unique :db.unique/identity}
   :var/namespace       {:db/type :db.type/ref}
   :var/current-value   {}
   :depends-on          {:db/type :db.type/ref :db/cardinality :db.cardinality/many}
   :defines             {:db/type :db.type/ref :db/cardinality :db.cardinality/many}
   :calls               {:db/type :db.type/ref :db/cardinality :db.cardinality/many}
   :analysis/timestamp  {:db/index true}
   :analysis/confidence {:db/index true}})

(defonce db-conn (d/create-conn schema))

;; Entity insertion
(defn add-namespace-analysis [ns-data]
  (d/transact! db-conn
    [[:db/add "ns" :entity/type :namespace]
     [:db/add "ns" :namespace/name (:name ns-data)]
     [:db/add "ns" :namespace/loaded (:loaded ns-data)]
     [:db/add "ns" :analysis/timestamp (System/currentTimeMillis)]
     [:db/add "ns" :analysis/confidence (:confidence ns-data)]]))

(defn add-var-analysis [var-data]
  (d/transact! db-conn
    [[:db/add "var" :entity/type :var]
     [:db/add "var" :var/fqn (:fqn var-data)]
     [:db/add "var" :var/current-value (pr-str (:current-value var-data))]
     [:db/add "var" :var/namespace [:namespace/name (namespace (:fqn var-data))]]]))

;; Complex queries become simple
(defn find-transitive-dependencies [namespace-name]
  "Find all namespaces transitively depended on by given namespace"
  (d/q '[:find [?dep-name ...]
         :in $ ?ns-name
         :where
         (or-join [?ns ?dep]
           (and [?ns :namespace/name ?ns-name]
                [?ns :depends-on ?dep])
           (and [?ns :namespace/name ?ns-name] 
                [?ns :depends-on ?intermediate]
                [?intermediate :depends-on ?dep]))
         [?dep :namespace/name ?dep-name]]
       @db-conn namespace-name))

(defn find-vars-calling-external-deps []
  "Find vars that call functions from external dependencies"
  (d/q '[:find ?var-name ?called-var ?called-ns
         :where
         [?var :entity/type :var]
         [?var :var/fqn ?var-name]
         [?var :calls ?called]
         [?called :var/fqn ?called-var]
         [?called :var/namespace ?called-ns-ent]
         [?called-ns-ent :namespace/name ?called-ns]
         [?var :var/namespace ?var-ns-ent]
         [?var-ns-ent :namespace/name ?var-ns]
         [(not= ?called-ns ?var-ns)]]
       @db-conn))

;; Time-based queries for change tracking
(defn namespace-changes-since [timestamp]
  "Find all namespace changes since timestamp"
  (d/q '[:find ?ns-name ?change-time
         :in $ ?since
         :where
         [?ns :entity/type :namespace]
         [?ns :namespace/name ?ns-name]
         [?ns :analysis/timestamp ?change-time]
         [(> ?change-time ?since)]]
       @db-conn timestamp))

;; Reactive queries for real-time updates
(def dependency-graph-query
  (d/listen! db-conn :dependency-graph
    (fn [tx-report]
      (when (some #(contains? #{:depends-on :defines :calls} (:a %)) 
                  (:tx-data tx-report))
        (update-dependency-visualization!)))))
```

**Use Cases Perfect for Datascript**:
```clojure
;; Complex relationship queries
(find-all-paths-between 'my.app.core 'clojure.java.io)
(find-circular-dependencies)
(find-unused-vars-in-namespace 'my.old.utils)

;; Historical analysis  
(namespace-state-at-time 'my.ns timestamp)
(var-redefinition-history 'my.ns/my-var)

;; Performance hotspot analysis
(most-called-functions-in-namespace 'my.app.core)
(functions-with-performance-issues {:min-time 100 :min-memory 10})
```

##### Option 2: Custom Graph Data Structures

**Advantages**:
- **Full Control**: Custom-tailored to exact needs
- **Performance**: Optimized for specific query patterns
- **No External Dependencies**: Pure Clojure implementation

```clojure
(ns clj-surveyor.graph
  "Custom graph storage for entity relationships")

(defrecord Entity [id type attributes relationships])
(defrecord Relationship [type from to attributes])

(defprotocol EntityGraph
  (add-entity [graph entity])
  (remove-entity [graph entity-id])
  (add-relationship [graph relationship])
  (find-entities [graph query])
  (traverse [graph start-entity relationship-types]))

(deftype InMemoryGraph [entities relationships indexes]
  EntityGraph
  (add-entity [graph entity]
    (let [id (:id entity)]
      (InMemoryGraph.
        (assoc entities id entity)
        relationships
        (update-indexes indexes entity))))
  
  (find-entities [graph {:keys [type attributes]}]
    (let [candidates (if type 
                      (get-in indexes [:by-type type])
                      (keys entities))]
      (filter #(matches-attributes? (get entities %) attributes)
              candidates)))
  
  (traverse [graph start-entity rel-types]
    (graph-traversal entities relationships start-entity rel-types)))

;; Usage
(def graph (create-empty-graph))

(-> graph
    (add-entity {:id "ns1" :type :namespace :name 'my.app.core})
    (add-entity {:id "var1" :type :var :fqn 'my.app.core/process})
    (add-relationship {:type :defines :from "ns1" :to "var1"}))

(traverse graph "ns1" [:defines :calls])
```

##### Option 3: Hybrid Approach (Maps + Datascript)

**Strategy**: Use maps for fast local access, Datascript for complex queries

```clojure
(ns clj-surveyor.hybrid-storage
  "Hybrid storage: maps for speed, datascript for queries")

;; Fast lookup maps  
(def namespace-cache (atom {}))
(def var-cache (atom {}))
(def dependency-cache (atom {}))

;; Datascript for complex relationships
(def query-db (datascript-conn))

(defn update-entity [entity]
  ;; Update fast cache
  (case (:type entity)
    :namespace (swap! namespace-cache assoc (:name entity) entity)
    :var (swap! var-cache assoc (:fqn entity) entity))
  
  ;; Update query database
  (d/transact! query-db [(entity->datascript-format entity)]))

;; Fast access patterns
(defn get-namespace [name] (@namespace-cache name))
(defn get-var [fqn] (@var-cache fqn))

;; Complex queries via datascript
(defn complex-dependency-analysis [root-ns]
  (d/q complex-query @query-db root-ns))
```

#### Recommendation: Start with Datascript

**Rationale**:

1. **Perfect Match**: Datalog is designed exactly for this type of entity-relationship modeling
2. **Query Power**: Complex traversals become simple datalog queries
3. **Immutable**: Fits Clojure philosophy, good for time-travel debugging
4. **Reactive**: Built-in reactivity for real-time updates
5. **Lightweight**: No external database needed
6. **Developer Experience**: Excellent REPL integration for query development

**Migration Path**: Start with Datascript, optimize with caching later if needed:

```clojure
;; Phase 1: Pure Datascript
(defn analyze-namespace [ns-sym]
  (let [analysis (runtime-analysis ns-sym)]
    (persist-to-datascript! analysis)
    (query-from-datascript ns-sym)))

;; Phase 2: Add caching for hot paths
(defn analyze-namespace [ns-sym]
  (or (get-cached-analysis ns-sym)
      (let [analysis (runtime-analysis ns-sym)]
        (persist-to-datascript! analysis)
        (cache-analysis! ns-sym analysis)
        analysis)))

;; Phase 3: Hybrid as needed
(defn get-var-info [fqn]
  ;; Fast path for simple lookups
  (or (@var-cache fqn)
      ;; Slow path for complex analysis
      (datascript-query-var fqn)))
```

**Implementation Considerations**:

**Schema Evolution**: Datascript schema can evolve as requirements grow
**Memory Management**: In-memory database, need cleanup strategies for long-running processes
**Persistence**: Consider occasional snapshots to disk for crash recovery
**Concurrent Access**: Datascript is thread-safe for reads, coordinate writes

### Data Models

#### Analysis Results
```clojure
;; Namespace analysis result
{:namespace 'my.app.core
 :loaded true
 :file "/path/to/file.clj"
 :dependencies [:clojure.string :clojure.set]
 :dependents [:my.app.handlers :my.app.routes]
 :public-vars [{:name 'process-data :type :function :arglists ...}]
 :protocols [{:name 'DataProcessor :methods [...]}]
 :multimethods [{:name 'handle-event :dispatch-fn ...}]
 :performance {:startup-time 45.2 :memory-usage 1.2}
 :quality-metrics {:documentation-coverage 0.85 :test-coverage 0.92}}

;; Function analysis result  
{:var #'my.app.core/process-data
 :type :function
 :arglists ([data] [data options])
 :documentation {...}
 :dependencies [:clojure.string/trim :my.app.utils/validate]
 :call-sites [{:namespace 'my.app.handlers :line 42}]
 :performance {:avg-call-time 2.3 :memory-allocation 512}
 :complexity {:cyclomatic 4 :cognitive 6}
 :quality-issues [{:type :reflection-warning :suggestion "Add type hints"}]}
```

#### Configuration
```clojure
;; Default configuration
{:collection 
 {:include-private false
  :include-protocols true  
  :include-multimethods true
  :performance-monitoring true}
 
 :analysis
 {:anti-pattern-detection true
  :style-checking {:guide :community}
  :complexity-analysis true
  :security-scanning false}
  
 :visualization
 {:format :html  ; :ascii, :html, :svg
  :max-depth 5
  :cluster-namespaces true}
  
 :web
 {:port 8080
  :host "localhost"
  :auth-required false
  :real-time-updates true}}
```

### Extension Points

#### Custom Analyzers
```clojure
(ns my.custom.analyzer
  (:require [clj-surveyor.analyze :as analyze]))

(defmethod analyze/custom-analysis :my-domain-analysis
  [code-element opts]
  ;; Custom analysis logic
  {:analysis-type :my-domain-analysis
   :results {...}})

;; Register the analyzer
(analyze/register-analyzer! :my-domain-analysis my-domain-analysis)
```

#### Custom Visualizations
```clojure
(ns my.custom.viz
  (:require [clj-surveyor.viz :as viz]))

(defmethod viz/render :my-custom-diagram
  [data format opts]
  ;; Custom visualization logic
  (case format
    :ascii (render-ascii-diagram data opts)
    :html (render-html-diagram data opts)))
```

## Implementation Plan

### Phase 1: Core Foundation (Months 1-2)
**Goal**: Establish entity storage system, dynamic state management, and basic analysis capabilities

**Deliverables**:
- [ ] Entity storage system (`clj-surveyor.db`) with Datascript backend
- [ ] Core entity schema and basic queries
- [ ] Dynamic state management (`clj-surveyor.state`) with entity change tracking
- [ ] Core introspection APIs (`clj-surveyor.collect`) with entity integration
- [ ] Change detection and entity update system
- [ ] Basic analysis functions (`clj-surveyor.analyze`) using entity queries
- [ ] Protocol and multimethod analysis stored as entities
- [ ] ASCII-based visualizations using entity relationships
- [ ] Comprehensive test suite including entity lifecycle scenarios
- [ ] Documentation and examples

**Key Features**:
- `(store-entity entity)` - Store runtime entities in Datascript database
- `(query-entities '[:find ?e :where [?e :entity/type :namespace]])` - Datalog queries
- `(analyze-namespace 'my.ns)` - Runtime namespace analysis stored as entities
- `(protocol-info IProtocol)` - Protocol entities with implementation relationships
- `(dependency-graph 'root.ns)` - ASCII visualization from entity relationships  
- `(entity-changes-since timestamp)` - Track entity modifications over time
- `(find-unused-vars scope)` - Complex relationship queries for analysis

**Dynamic State Management Features**:
- Runtime change detection and automatic cache invalidation
- Incremental analysis updates when vars are redefined
- Confidence levels for analysis results based on staleness
- REPL integration with automatic monitoring of `eval` operations
- Graceful handling of concurrent analysis during runtime changes

### Phase 2: Web Interface (Months 3-4)
**Goal**: Replace clj-ns-browser with modern web-based interface powered by entity queries

**Deliverables**:
- [ ] Web server with RESTful API (`clj-surveyor.web`) exposing entity endpoints
- [ ] Modern HTML/CSS/JS frontend with datalog query builder
- [ ] Real-time updates via WebSockets connected to reactive entity queries
- [ ] Interactive entity-relationship visualizations (D3.js/similar)
- [ ] Advanced search and filtering using entity attributes and relationships
- [ ] Historical entity timeline views
- [ ] Collaborative entity annotation and bookmarking
- [ ] Export functionality (PDF, PNG, SVG)

**Key Features**:
- Modern, responsive web UI
- Interactive dependency graphs
- Real-time namespace monitoring
- Advanced search with fuzzy matching
- Collaborative annotations and notes

### Phase 3: Advanced Analysis (Months 5-6)
**Goal**: Integrate advanced analysis and external tool connectivity

**Deliverables**:
- [ ] Performance monitoring integration
- [ ] Static analysis tool integration (clj-kondo, eastwood)
- [ ] Code quality metrics and suggestions
- [ ] Memory usage analysis
- [ ] Security vulnerability detection
- [ ] AI-powered code explanations (optional)

**Key Features**:
- Performance hotspot identification
- Anti-pattern detection
- Style guide compliance checking
- Memory leak detection
- Security vulnerability scanning

### Phase 4: Ecosystem Integration (Months 7-8)
**Goal**: Integrate with development tools and workflows

**Deliverables**:
- [ ] nREPL middleware
- [ ] LSP server integration
- [ ] Leiningen plugin
- [ ] deps.edn tooling integration
- [ ] CI/CD pipeline integration
- [ ] VS Code / Emacs / IntelliJ plugins

**Key Features**:
- Editor hover information
- Enhanced auto-completion
- Build-time analysis reports
- Continuous quality monitoring
- Team collaboration features

## Migration Path from Existing Tools

### From clj-info
1. **Preserve All Existing APIs**: Ensure 100% backward compatibility
2. **Enhance Output Formats**: Add new visualization options while maintaining existing ones
3. **Extend ClojureDocs Integration**: Build on v0.6.0 ClojureDocs features
4. **Performance**: Maintain or improve performance characteristics

### From clj-ns-browser  
1. **Feature Parity**: Implement all existing clj-ns-browser features in web interface
2. **Data Migration**: Provide tools to migrate settings and preferences
3. **Transition Guide**: Detailed migration documentation
4. **Parallel Operation**: Allow both tools to coexist during transition

### Integration Strategy
```clojure
;; Unified API that works with both legacy and new features
(require '[clj-surveyor.unified :as survey])

;; Legacy clj-info compatibility
(survey/info 'map)  ; Works exactly like clj-info/info

;; Legacy clj-ns-browser compatibility  
(survey/browse)     ; Opens web interface instead of Swing

;; New capabilities
(survey/analyze-codebase {:root-ns 'my.app
                         :include-deps true
                         :output-format :web})
```

## Technical Considerations

### Performance
- **Lazy Loading**: Only analyze code when requested
- **Caching**: Intelligent caching of analysis results
- **Incremental Updates**: Only re-analyze changed code
- **Background Processing**: Long-running analysis in background threads

### Memory Management
- **Weak References**: For caching to allow garbage collection
- **Streaming**: Large datasets streamed rather than held in memory
- **Configurable Limits**: Memory usage limits and cleanup

### Security  
- **Sandboxed Analysis**: Safe analysis of untrusted code
- **Access Controls**: Configurable access controls for web interface
- **Data Privacy**: No external data transmission without explicit consent

### Cross-Platform Compatibility
- **JVM Clojure**: Full feature support
- **Babashka**: Core features with graceful degradation
- **ClojureScript**: Web interface and basic analysis
- **GraalVM**: Native image compatibility where possible

## Success Metrics

### Adoption Metrics
- Downloads and usage statistics
- Community feedback and contributions
- Integration by other tools and libraries

### Technical Metrics  
- Performance benchmarks vs existing tools
- Memory usage efficiency
- Analysis accuracy and completeness
- Test coverage and code quality

### User Experience Metrics
- Time to insights for new codebases
- Feature discovery and usage patterns
- User satisfaction surveys
- Issue resolution times

## Future Vision

### Long-term Goals (Year 2+)
1. **AI Integration**: Machine learning for code pattern recognition
2. **Collaborative Platform**: Team-based code exploration and documentation
3. **Language Expansion**: Support for other JVM languages (Scala, Kotlin)
4. **Cloud Integration**: Cloud-based analysis for large codebases
5. **Educational Platform**: Interactive learning modules for Clojure concepts

### Research Areas
1. **Program Synthesis**: AI-assisted code generation based on analysis
2. **Formal Verification**: Integration with formal verification tools
3. **Real-time Optimization**: Runtime optimization suggestions
4. **Cross-Language Analysis**: Analysis across multiple programming languages

## Research: Similar Tools and Ecosystem Analysis

### Existing Clojure Code Analysis Tools

Based on research of the Clojure ecosystem, several excellent tools provide complementary capabilities that clj-surveyor can build upon:

#### 1. **clj-kondo** - Static Analyzer and Linter
- **Strengths**: Comprehensive linting rules, excellent error detection, fast static analysis
- **Architecture**: Uses custom parser, provides detailed analysis data, supports editor integration
- **Reusable Components**: 
  - Analysis data format and schema design patterns
  - Configuration system approach (`.clj-kondo` directory)
  - Caching strategies for performance
- **Integration Opportunity**: Use clj-kondo's analysis data as input for entity storage

#### 2. **tools.analyzer(.jvm)** - AST Analysis Framework  
- **Strengths**: Official Clojure AST analysis, highly accurate, extensible pass system
- **Architecture**: Multi-pass analysis with customizable passes, generic AST walking
- **Reusable Components**:
  - AST node structure and metadata patterns
  - Pass scheduling and dependency management
  - Generic tree walking algorithms (`ast/children`, `ast/nodes`, `ast/prewalk`)
- **Integration Opportunity**: Use as foundation for runtime introspection and AST analysis

#### 3. **clojure-lsp** - Language Server Implementation
- **Strengths**: Real-time analysis, editor integration, refactoring capabilities
- **Architecture**: Runtime analysis with incremental updates, LSP protocol implementation
- **Reusable Components**:
  - Real-time change detection patterns
  - Symbol resolution and cross-reference tracking
  - Editor integration protocols
- **Integration Opportunity**: Share analysis data format, learn from real-time update strategies

#### 4. **Edamame** - Configurable Parser with Location Metadata
- **Strengths**: Location-aware parsing, highly configurable, handles various Clojure dialects
- **Architecture**: Parser focused on preserving source location information
- **Reusable Components**:
  - Location metadata attachment strategies
  - Configurable reader options
  - Error handling with source position information
- **Integration Opportunity**: Use for parsing with precise source locations for entity attribution

#### 5. **Eastwood** - Runtime-based Linter
- **Strengths**: Deep analysis through runtime evaluation, extensive linter rules
- **Architecture**: Uses tools.analyzer for analysis, runtime evaluation for accuracy  
- **Reusable Components**:
  - Runtime evaluation strategies for analysis
  - Extensive linter rule implementations
  - Configuration and suppression systems
- **Integration Opportunity**: Learn from runtime analysis approach, reuse linter rule patterns

#### 6. **cljfmt** - Code Formatter
- **Strengths**: Fast formatting, configurable rules, editor integration
- **Architecture**: Parse-and-rewrite approach with rule-based formatting
- **Reusable Components**:
  - Configuration file discovery (`.cljfmt.edn`)
  - Rule-based transformation patterns
  - Editor integration strategies
- **Integration Opportunity**: Use formatting rules for code quality analysis

#### 7. **zprint** - Advanced Code Formatter  
- **Strengths**: Sophisticated formatting options, style configurations, performance optimization
- **Architecture**: AST-based with extensive configuration options
- **Reusable Components**:
  - Style configuration systems
  - Performance optimization techniques
  - Output formatting strategies
- **Integration Opportunity**: Leverage configuration patterns for presentation layer

#### 8. **Joker** - Clojure Interpreter and Linter
- **Strengths**: Fast startup, lightweight, standalone linting capability
- **Architecture**: Go-based implementation with Clojure compatibility
- **Reusable Components**:
  - Fast startup optimization strategies  
  - Standalone deployment patterns
  - Configuration file patterns (`.joker`)
- **Integration Opportunity**: Learn from performance optimization approaches

### Strategic Integration Opportunities

#### Code Reuse Potential:
1. **Parser Layer**: Edamame for location-aware parsing
2. **AST Analysis**: tools.analyzer for accurate analysis passes  
3. **Static Analysis**: clj-kondo analysis data as input source
4. **Configuration**: Adopt proven configuration patterns (`.clj-kondo`, `.cljfmt.edn`)
5. **Performance**: Learn from joker's optimization strategies

#### Data Format Compatibility:
- **clj-kondo analysis data** → entity storage input
- **tools.analyzer AST** → entity relationship source
- **clojure-lsp symbols** → cross-reference data
- **Standardized location metadata** from edamame patterns

#### Architectural Learning:
- **Incremental analysis** from clojure-lsp real-time updates
- **Pass-based analysis** from tools.analyzer extensibility
- **Caching strategies** from clj-kondo performance optimizations
- **Configuration discovery** from multiple tools' approaches

#### Integration Architecture:
```clojure
;; clj-surveyor as analysis coordinator
(defn analyze-codebase [project-root]
  (let [;; Leverage existing tools
        kondo-analysis (clj-kondo/analysis project-root)
        lsp-symbols (clojure-lsp/extract-symbols project-root)
        
        ;; Convert to entities
        entities (-> kondo-analysis
                     (merge-lsp-data lsp-symbols)
                     (convert-to-entities))
        
        ;; Store in Datascript
        db (populate-entity-db entities)]
    
    ;; Provide enhanced analysis on top
    (enhanced-analysis db)))
```

### Differentiation Strategy:

**clj-surveyor's Unique Value**:
- **Entity-centric analysis**: First tool to treat all code elements as queryable entities
- **Runtime-first approach**: Prioritizes live system state over static files  
- **Cross-tool integration**: Coordinates and enhances existing tools rather than replacing them
- **Relationship-focused**: Uses datalog queries for complex dependency analysis
- **Historical tracking**: Maintains entity change history over time

Rather than competing with existing tools, clj-surveyor can serve as an **analysis coordinator** that enhances and integrates the Clojure tooling ecosystem while providing unique entity-relationship insights not available elsewhere.

## Getting Started: Quick Implementation Guide

For developers wanting to begin implementation, start with this minimal entity storage foundation:

```clojure
(ns clj-surveyor.core
  (:require [datascript.core :as d]))

;; Enhanced schema reflecting FQN-based dependency insight
(def entity-schema
  {:entity/id       {:db/unique :db.unique/identity}
   :entity/type     {:db/index true}
   :entity/name     {:db/index true}  
   :entity/fqn      {:db/unique :db.unique/identity} ; Fully Qualified Name
   :entity/updated  {:db/index true}
   
   ;; True dependencies: actual var usage, not requires
   :var/uses        {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   :var/used-by     {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   
   ;; Convenience relationships (requires/refers)  
   :ns/requires     {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   :ns/refers       {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   
   ;; Usage context
   :usage/location  {:db/cardinality :db.cardinality/many}  ; Where FQN is used
   :usage/count     {}                                      ; How many times used
   :usage/dynamic?  {}})                                    ; Found at runtime vs static

;; Initialize database
(def db (atom (d/empty-db entity-schema)))

;; Basic entity operations
(defn add-entity! [entity]
  (swap! db d/db-with [entity]))

(defn find-entities [type]
  (d/q '[:find ?e ?name
         :in $ ?type  
         :where [?e :entity/type ?type]
                [?e :entity/name ?name]]
       @db type))

;; Example: Store entities reflecting true dependencies

;; Store a var with its actual usage
(add-entity! {:entity/id     "my.app/process"
              :entity/type   :var
              :entity/name   "process"
              :entity/fqn    'my.app/process
              :entity/updated (java.util.Date.)})

;; Store var usage relationships (the REAL dependencies)
(add-entity! {:entity/id     "usage-1"
              :entity/type   :usage
              :var/uses      #{"clojure.string/upper-case" "my.utils/helper-fn"}
              :usage/location {:file "src/my/app.clj" :line 10 :column 3}
              :usage/count   2})

;; Traditional namespace requires (convenience only)
(add-entity! {:entity/id     "my.app-requires"
              :entity/type   :namespace-meta
              :ns/requires   #{"clojure.string" "my.utils"}
              :ns/refers     #{"helper-fn"}})

;; Query true var-level dependencies
(d/q '[:find ?user ?used
       :where 
       [?u :var/uses ?used-entity]
       [?used-entity :entity/fqn ?used]
       [?u :entity/fqn ?user]]
     @db)
;; => [['my.app/process 'clojure.string/upper-case]
;;     ['my.app/process 'my.utils/helper-fn]]

;; Find unused requires (requires not reflected in actual usage)
(d/q '[:find ?required-ns
       :where
       [?ns :ns/requires ?required-ns]
       (not [?usage :var/uses ?var]
            [?var :entity/fqn ?fqn]
            [(clojure.string/starts-with? (str ?fqn) (str ?required-ns))])]
     @db)
```

This foundation provides the core entity storage system that all other modules build upon. From here, implement data collection to populate entities from runtime introspection, then add analysis functions that query the entity database.

## Conclusion

clj-surveyor represents the next evolution in Clojure development tooling, combining the best aspects of clj-info's documentation capabilities and clj-ns-browser's exploration features with modern analysis techniques and web-based interfaces. By focusing on runtime analysis, extensibility, and developer productivity, clj-surveyor aims to become the definitive tool for understanding and navigating Clojure codebases.

The phased implementation approach ensures steady progress while maintaining compatibility with existing tools, allowing for a smooth transition and adoption process. The extensible architecture provides a foundation for future enhancements and community contributions, ensuring the tool can evolve with the Clojure ecosystem.

---

*This document is a living specification and will be updated as development progresses and requirements evolve.*