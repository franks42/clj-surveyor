# clj-surveyor: Entity-Graph Analysis for Clojure Codebases

## The Big Idea

**clj-surveyor models Clojure codebases as a queryable entity-relationship graph**, where insights emerge from analyzing slices and patterns within this graph rather than individual files or namespaces.

### Core Insight: The Clojure Development Paradox

Clojure promotes **immutability** in application code but development itself is **highly mutable** - vars are redefined, namespaces reloaded, dependencies change dynamically. This invisible runtime mutability causes bugs and surprises that file-based tools cannot detect.

### The Time Component: Always Out of Sync

**Critical Reality**: The entity graph is **always an imperfect reflection** of the current runtime state. By the time we capture and store entities, **the runtime has already moved on**:

```clojure
;; Time T0: Capture entity graph
{:var/fqn 'my.app/process :var/source "(defn process [x] (inc x))"}

;; Time T1: Developer redefines in REPL (we miss this!)
(defn process [x] (dec x))  ; Different behavior, same name

;; Time T2: Our analysis runs on T0 data
;; => We analyze the OLD definition while code uses the NEW one
```

**The Fundamental Challenge**: Runtime is **continuously changing** while analysis takes **non-zero time**. We're always chasing a moving target.

**clj-surveyor's approach**: 
1. **Acknowledge imperfection** - embrace "good enough" temporal snapshots
2. **Track staleness** - timestamp everything and show data age  
3. **Incremental updates** - minimize sync lag through fast change capture
4. **Historical context** - use time-series data to understand change patterns

## Entity-Centric Architecture

### The Entity Universe

Every element in a Clojure codebase becomes a **first-class entity** with:
- **Identity**: Unique identification (typically FQN)  
- **Attributes**: Properties and metadata
- **Relationships**: Connections to other entities
- **History**: Evolution over time

### Core Entity Types

#### 1. **Var Entities** (with Temporal Metadata)
```clojure
{:entity/type :var
 :entity/fqn  'my.app/process-user
 :var/name    "process-user"
 :var/ns      'my.app
 :var/doc     "Processes a user record..."
 :var/arglists '([user] [user opts])
 :var/source  "(defn process-user [user] ...)"
 :var/line    42
 :var/file    "src/my/app.clj"
 :var/macro?  false
 :var/private? false
 
 ;; Temporal tracking - acknowledge imperfection
 :entity/captured-at    #inst "2024-10-29T15:30:15.123Z"
 :entity/staleness-ms   1247  ; milliseconds since capture
 :entity/confidence     0.85  ; how sure are we this is current?
 :entity/last-changed   #inst "2024-10-29T15:29:00.000Z"
 :entity/sync-lag       true  ; runtime may have moved on
 :entity/version        17}   ; incremental update counter
```

#### 2. **Namespace Entities**
```clojure
{:entity/type :namespace
 :entity/fqn  'my.app
 :ns/name     "my.app"
 :ns/file     "src/my/app.clj"
 :ns/doc      "Main application logic"
 :ns/requires ['clojure.string 'my.utils]
 :ns/refers   ['helper-fn]
 :ns/aliases  {'str 'clojure.string}}
```

#### 3. **Usage Entities** (The Key Innovation)
```clojure
{:entity/type :usage
 :usage/caller    'my.app/process-user
 :usage/callee    'clojure.string/upper-case
 :usage/location  {:file "src/my/app.clj" :line 45 :column 12}
 :usage/context   :function-body
 :usage/dynamic?  false
 :usage/count     3}
```

#### 4. **Dependency Entities** 
```clojure
{:entity/type :dependency
 :dep/from    'my.app
 :dep/to      'clojure.string  
 :dep/type    :namespace-require
 :dep/alias   'str
 :dep/refers  []
 :dep/convenience? true}  ; vs actual usage
```

#### 5. **Change Event Entities** (Temporal Streams)
```clojure
{:entity/type :change-event
 :change/target      'my.app/process-user
 :change/type        :var-redefinition
 :change/timestamp   #inst "2024-10-29T15:30:00.456Z"
 :change/source      :repl-eval
 :change/diff        {:added-lines 3 :removed-lines 1}
 
 ;; Temporal context
 :change/detection-lag  23   ; ms between actual change and detection
 :change/missed-changes 0    ; estimated changes we didn't catch
 :change/sequence-id    1247 ; order in change stream
 :change/causality-gap? false ; did we miss intermediate changes?}
```

#### 6. **Staleness Entities** (New: Track Sync State)
```clojure
{:entity/type :staleness-info
 :staleness/scope     'my.app        ; namespace or var
 :staleness/last-sync #inst "2024-10-29T15:30:00Z"
 :staleness/age-ms    2341
 :staleness/confidence 0.72          ; 0.0 = totally stale, 1.0 = perfect sync
 :staleness/drift-rate 0.05          ; changes per second
 :staleness/warning?   true}         ; beyond acceptable staleness threshold
```

### The Entity Relationship Graph

Entities connect through **typed relationships**:

```clojure
;; Var relationships
:var/defined-in    ; var -> namespace
:var/uses          ; var -> var (FQN usage)
:var/used-by       ; var -> var (reverse)
:var/calls         ; var -> var (function calls)
:var/refers-to     ; var -> var (symbol references)

;; Namespace relationships  
:ns/contains       ; namespace -> var
:ns/requires       ; namespace -> namespace
:ns/depends-on     ; namespace -> namespace (via usage)

;; Usage relationships
:usage/in-context  ; usage -> context entity
:usage/from-var    ; usage -> calling var
:usage/to-var      ; usage -> called var

;; Temporal relationships
:change/previous   ; change -> previous change
:change/affects    ; change -> affected entities
```

## Query-Driven Insights

### The Analysis Philosophy

**Insights emerge from querying entity relationships**, not analyzing individual components. Each use case translates to specific datalog queries that reveal patterns in the entity graph.

### Temporal Query Patterns: Working with Imperfect Data

**Every query must acknowledge temporal uncertainty**. All analysis includes staleness metadata and confidence levels.

#### Staleness-Aware Queries
```clojure
;; Standard query with temporal context
'[:find ?var ?confidence ?age
  :where 
  [?var :var/fqn 'my.app/process-user]
  [?var :entity/confidence ?confidence]
  [?var :entity/staleness-ms ?age]
  [(> ?confidence 0.8)]    ; only high-confidence results
  [(< ?age 5000)]]         ; fresher than 5 seconds

;; Change velocity analysis
'[:find ?var ?change-rate
  :where
  [?change :change/target ?var]
  [?change :change/timestamp ?when]
  [(count ?change) ?change-count]
  [(/ ?change-count time-window) ?change-rate]]
```

### Core Query Patterns

#### 1. **True Dependencies** (FQN Usage vs Requires)
```clojure
;; What does my.app actually depend on?
'[:find ?dep-ns (count ?usage)
  :where 
  [?var :var/ns 'my.app]
  [?usage :usage/caller ?var]
  [?usage :usage/callee ?dep-var]
  [?dep-var :var/ns ?dep-ns]
  (not= ?dep-ns 'my.app)]

;; Unused requires (convenience without usage)  
'[:find ?required-ns
  :where
  [?ns :ns/requires ?required-ns]
  (not-exists [?usage :usage/callee ?var]
              [?var :var/ns ?required-ns])]
```

#### 2. **Impact Analysis**
```clojure
;; What breaks if I change this var?
'[:find ?affected-var ?usage-count
  :where
  [?usage :usage/callee 'my.utils/helper-fn]
  [?usage :usage/caller ?affected-var]
  [?affected-var :var/ns ?ns]
  [(count ?usage) ?usage-count]]

;; Transitive dependencies  
'[:find ?transitive-dep
  :where
  [?var :var/fqn 'my.app/main]
  (or-join [?var ?transitive-dep]
    [?var :var/uses ?transitive-dep]
    (and [?var :var/uses ?intermediate]
         [?intermediate :var/uses ?transitive-dep]))]
```

#### 3. **Architecture Analysis**
```clojure
;; Circular dependencies
'[:find ?ns1 ?ns2
  :where
  [?var1 :var/ns ?ns1]
  [?var2 :var/ns ?ns2]  
  [?var1 :var/uses ?var2]
  [?var2 :var/uses ?var1]
  (not= ?ns1 ?ns2)]

;; Coupling metrics
'[:find ?ns (count ?external-deps)
  :where
  [?var :var/ns ?ns]
  [?var :var/uses ?dep-var]
  [?dep-var :var/ns ?external-ns]
  (not= ?ns ?external-ns)]
```

#### 4. **Development History**
```clojure
;; What changed during this session?
'[:find ?var ?change-count
  :in $ ?since
  :where
  [?change :change/target ?var]
  [?change :change/timestamp ?when]
  [(> ?when ?since)]
  [(count ?change) ?change-count]]

;; Runtime vs file diff
'[:find ?var ?runtime-version ?file-version  
  :where
  [?var :var/runtime-source ?runtime-version]
  [?var :var/file-source ?file-version]
  (not= ?runtime-version ?file-version)]
```

## Use Case → Query Mapping

### 1. **"What does this namespace really depend on?"**
**Query**: Find all vars used by namespace vars, group by target namespace
**Display**: Dependency tree with usage counts and specific vars used

### 2. **"Can I safely remove this function?"**  
**Query**: Find all usages of the function across the codebase
**Display**: Impact visualization showing affected code with change propagation

### 3. **"Why is my function behaving differently than expected?"**
**Query**: Compare runtime var definition with file content, show change history **with staleness warnings**
**Display**: Timeline of redefinitions with diffs, sources, and **temporal confidence indicators**

### 4. **"How tightly coupled is this codebase?"**
**Query**: Calculate coupling metrics between namespaces via var usage
**Display**: Coupling heatmap and architectural suggestions

### 5. **"What are the unused parts of this codebase?"**
**Query**: Find vars with no incoming usage relationships
**Display**: Dead code report with safe-to-remove recommendations

## Temporal Challenges & Pragmatic Solutions

### The Synchronization Dilemma

**Problem**: Runtime changes faster than we can observe and record. Every analysis is based on **slightly stale data**.

**Pragmatic Approaches**:

#### 1. **Embrace "Good Enough" Analysis**
```clojure
;; Always show data freshness in UI
"Dependencies of my.app (data from 2.3 seconds ago, confidence: 0.87)"
"⚠️  High change velocity detected - results may be stale"
```

#### 2. **Staleness Budgets** 
```clojure
(def staleness-thresholds
  {:critical-analysis  1000   ; 1 second max for safety-critical queries
   :general-exploration 5000  ; 5 seconds OK for browsing  
   :historical-trends  30000  ; 30 seconds fine for trends
   :architecture-view  300000}) ; 5 minutes acceptable for big picture
```

#### 3. **Change Velocity Awareness**
```clojure
;; Adjust confidence based on how fast things are changing
(defn calculate-confidence [entity-age change-velocity]
  (let [decay-rate (* change-velocity 0.1)]
    (max 0.0 (- 1.0 (* entity-age decay-rate)))))
```

#### 4. **Incremental Sync Strategies**
- **Hot entities**: Sync frequently changed vars every 100ms
- **Cold entities**: Sync stable components every 10s  
- **Batch updates**: Process change streams in temporal windows
- **Priority queues**: Update critical paths first

### Making Uncertainty Visible

**UI Patterns**:
- 🟢 **Fresh** (< 1s): Full confidence in results
- 🟡 **Aging** (1-5s): Probably accurate, show age
- 🟠 **Stale** (5-30s): Likely outdated, warn user
- 🔴 **Ancient** (>30s): Definitely wrong, block analysis

**Query Results Always Include**:
```clojure
{:results [...analysis data...]
 :meta {:freshness-ms 2340
        :confidence 0.73
        :warning "High-velocity changes detected"
        :recommendation "Re-run analysis for current state"}}
```

## Implementation Strategy

### Phase 1: Entity Storage Foundation
```clojure
(ns clj-surveyor.core
  (:require [datascript.core :as d]))

;; Entity schema optimized for queries
(def schema
  {:entity/fqn      {:db/unique :db.unique/identity}
   :var/uses        {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   :usage/caller    {:db/valueType :db.type/ref}
   :usage/callee    {:db/valueType :db.type/ref}
   :ns/requires     {:db/valueType :db.type/ref :db/cardinality :db.cardinality/many}
   :change/target   {:db/valueType :db.type/ref}
   :change/previous {:db/valueType :db.type/ref}})
```

### Phase 2: Runtime Data Collection (Temporal Challenges)
- **Hook into var redefinition** to capture change events (acknowledge detection lag)
- **Parse loaded namespaces** to extract entities (snapshot in time, immediately stale)
- **Analyze usage patterns** from runtime metadata (racing against changes)
- **Track require/refer statements** as convenience relationships
- **Implement staleness tracking** - timestamp everything, measure sync lag
- **Change stream processing** - capture temporal sequence of mutations
- **Confidence scoring** - estimate how current our data actually is

### Phase 3: Query Interface & Visualization
- **Datalog query builder** for interactive exploration
- **Pre-built query library** for common use cases  
- **Graph visualizations** of entity relationships
- **Timeline views** of development history

### Phase 4: Integration & Tooling
- **Web interface** replacing clj-ns-browser with entity-powered UI
- **Editor integrations** with query-based code intelligence
- **CI/CD analysis** for architecture and dependency health
- **Team collaboration** features for shared codebase understanding

## Differentiation from Existing Tools

| **Tool** | **Focus** | **clj-surveyor Enhancement** |
|----------|-----------|------------------------------|
| **clj-kondo** | Static linting | **+ Runtime entity tracking** |
| **clojure-lsp** | Editor features | **+ Historical analysis & queries** |  
| **clj-ns-browser** | Individual symbol exploration | **+ Relationship analysis & dependencies** |
| **tools.analyzer** | AST analysis | **+ Entity persistence & querying** |

**Unique Value**: clj-surveyor is the **only tool that treats codebase elements as queryable entities with relationships and history**, enabling insights impossible with traditional approaches.

## Success Metrics

1. **Query Response Time**: <100ms for typical relationship queries
2. **Entity Coverage**: Capture 99%+ of runtime var definitions and usages  
3. **Memory Efficiency**: <50MB overhead for typical medium-sized projects
4. **Developer Adoption**: Reduce time-to-understand-codebase by 50%+
5. **Bug Prevention**: Catch runtime-vs-file mismatches before they cause issues

---

*clj-surveyor transforms Clojure development from "code archaeology" to "intelligent exploration" through entity-relationship analysis and query-driven insights.*