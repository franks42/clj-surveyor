# clj-surveyor: Entity-Graph Analysis for Clojure Codebases (v3)

## The Big Idea

**clj-surveyor models Clojure codebases as a queryable entity-relationship graph with temporal awareness**, where insights emerge from analyzing patterns and relationships rather than individual files or namespaces.

### Core Insight: The Clojure Development Paradox

Clojure promotes **immutability** in application code but development itself is **highly mutable** - vars are redefined, namespaces reloaded, dependencies change dynamically. This invisible runtime mutability causes bugs and surprises that file-based tools cannot detect.

### Validation: The Smalltalk Heritage

**clj-ns-browser** proved that Clojure developers value runtime-first tools inspired by Smalltalk's live system browsers. clj-surveyor is the next evolution: it **externalizes the implicit model into a queryable, persistent, historical database**.

This is not a new, unproven idea—it's the natural successor to a validated paradigm.

## The Temporal Reality: Always Out of Sync

**Critical Insight**: The entity graph is **always an imperfect reflection** of the current runtime state. By the time we capture and store entities, **the runtime has already moved on**.

**clj-surveyor's honest approach**: 
1. **Acknowledge imperfection** - embrace "good enough" temporal snapshots
2. **Track staleness** - timestamp everything and show data age  
3. **Propagate confidence** - every query result includes trustworthiness indicators
4. **Historical context** - use event streams to understand causality and change patterns

## Enhanced Entity Architecture

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
 
 ;; Temporal tracking
 :entity/captured-at    #inst "2024-10-29T15:30:15.123Z"
 :entity/staleness-ms   1247
 :entity/confidence     0.85
 :entity/version        17}
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
 :usage/count     3          ; NEW: frequency tracking
 :usage/frequency-hz 0.5}    ; NEW: calls per second (runtime sampling)
```

#### 4. **Event Entities** (Expanded from Change Events)

**Key Enhancement**: Generalize beyond var changes to capture full development lifecycle.

```clojure
{:entity/type :event
 :event/type  #{:var-redefinition :test-run :require-invoked 
                :load-file :profiling-sample :repl-command}
 :event/target      'my.app/process-user
 :event/timestamp   #inst "2024-10-29T15:30:00.456Z"
 :event/source      :repl-eval
 
 ;; Causality tracking (NEW)
 :event/triggered-by    {:db/valueType :db.type/ref}  ; parent event
 :event/cascade-depth   3                             ; nth-order effect
 :event/causality-confidence 0.8
 
 ;; Temporal context
 :event/detection-lag   23      ; ms between actual change and detection
 :event/sequence-id     1247}   ; order in event stream
```

#### 5. **Intent Entities** (NEW: Human-in-the-Loop)

**Feedback Insight**: Some context (why a change was made) cannot be captured automatically.

```clojure
{:entity/type :intent
 :intent/author "dev@team.com"
 :intent/timestamp #inst "2024-10-29T15:30:00Z"
 :intent/description "Refactoring for performance"
 :intent/affected-entities ['my.app/process-user 'my.app/validate]
 :intent/expected-outcome "50% faster processing"
 :intent/actual-outcome {:measured true :improvement 0.47}
 :intent/status :verified}
```

#### 6. **File Entities** (NEW: Eval Order Tracking)

**Critical Insight**: Files represent an **ordered sequence of evaluations**, not just collections of definitions. Side effects and dependencies mean **sequence matters**.

```clojure
{:entity/type :file
 :file/path "src/my/app.clj"
 :file/namespace 'my.app
 :file/statements [{:statement/id 1 :statement/type :def :statement/var 'config}
                   {:statement/id 2 :statement/type :defn :statement/var 'process}
                   {:statement/id 3 :statement/type :alter-var-root :statement/var 'config}
                   {:statement/id 4 :statement/type :defn :statement/var 'process-slow}]
 :file/eval-order [1 2 3 4]  ; Canonical sequence
 :file/last-modified #inst "2024-10-29T15:30:00Z"
 
 ;; Temporal tracking
 :file/last-evaled #inst "2024-10-29T15:30:15Z"
 :file/eval-lag-ms 15000      ; File changed but not re-evaled
 :file/partial-eval? true     ; Some statements evaled individually in REPL
 :file/confidence 0.65}       ; Runtime may differ from file
```

#### 7. **Statement/Form Entities** (NEW: Top-Level Forms)

**The File-as-Sequence Problem**: Individual top-level forms have dependencies, side effects, and ordering constraints that must be tracked.

```clojure
{:entity/type :statement
 :statement/id 42
 :statement/file "src/my/app.clj"
 :statement/namespace 'my.app
 :statement/line 10
 :statement/type :defn  ; or :def, :alter-var-root, :require, :side-effect
 :statement/source "(defn process [x] ...)"
 :statement/defines 'my.app/process  ; What var it creates/modifies
 
 ;; Dependency tracking
 :statement/depends-on ['my.app/config 'clojure.string/upper-case]
 :statement/side-effects #{:io :var-mutation :state-change}
 :statement/purity :pure | :pure-with-forward-declare | :impure
 
 ;; Sequencing constraints
 :statement/position 2          ; Position in file
 :statement/must-follow [1]     ; Must eval after statement 1
 :statement/can-reorder? false  ; Safe to eval in any order?
 
 ;; Temporal tracking
 :statement/last-evaled #inst "2024-10-29T15:30:00Z"
 :statement/eval-count 3        ; Evaled 3 times (REPL experiments)
 :statement/eval-source :repl-individual}  ; vs :file-load
```

**Side Effect Classification**:
```clojure
:statement/side-effects
  #{:io              ; Reads/writes external data
    :var-mutation    ; alter-var-root, def, defonce
    :state-change    ; swap!, reset!, alter, etc.
    :resource-creation  ; database connections, file handles
    :require}        ; Loads other namespaces
```

**Purity Levels**:
- **`:pure`** - No side effects, only defines var (can reorder freely)
- **`:pure-with-forward-declare`** - Pure after forward declarations added
- **`:impure`** - Has side effects, order-dependent

#### 8. **Staleness Entities** (Confidence Tracking)
```clojure
{:entity/type :staleness-info
 :staleness/scope     'my.app
 :staleness/last-sync #inst "2024-10-29T15:30:00Z"
 :staleness/age-ms    2341
 :staleness/confidence 0.72
 :staleness/drift-rate 0.05        ; changes per second
 :staleness/warning?   true}
```

### The Entity Relationship Graph

Enhanced with **confidence propagation** and **eval order tracking**:

```clojure
;; Standard relationships
:var/uses          ; var -> var (FQN usage)
:var/used-by       ; var -> var (reverse)
:ns/requires       ; namespace -> namespace
:ns/depends-on     ; namespace -> namespace (via actual usage)

;; Temporal relationships
:event/previous    ; event -> previous event (temporal chain)
:event/triggered-by ; event -> causative event (causality)
:event/affects     ; event -> affected entities

;; Intent relationships
:intent/affects    ; intent -> entities
:intent/achieved-by ; intent -> events that fulfilled it

;; File/Statement relationships (NEW)
:file/contains     ; file -> statements (ordered collection)
:statement/in-file ; statement -> file
:statement/defines ; statement -> var (what it creates/modifies)
:statement/depends-on ; statement -> vars (what it uses)
:statement/precedes   ; statement -> statement (eval order constraint)
```

## Query-Driven Insights with Confidence

### The Analysis Philosophy

**Insights emerge from querying entity relationships with temporal awareness**. Every query includes staleness metadata and confidence levels.

### Enhanced Query Patterns

#### 1. **Staleness-Aware Queries**
```clojure
;; Query with confidence propagation
'[:find ?var ?confidence ?age
  :where 
  [?var :var/fqn 'my.app/process-user]
  [?var :entity/confidence ?confidence]
  [?var :entity/staleness-ms ?age]
  [(> ?confidence 0.8)]    ; only high-confidence results
  [(< ?age 5000)]]         ; fresher than 5 seconds

;; Result includes confidence metadata
{:results [{:var 'my.app/process-user ...}]
 :meta {:freshness-ms 2340
        :confidence 0.73                    ; MINIMUM confidence in result set
        :confidence-distribution {:high 0.2 :medium 0.5 :low 0.3}
        :weak-links [{:entity "my.utils/helper" :confidence 0.15}]
        :recommendation "Re-analyze my.utils/helper for better confidence"}}
```

#### 2. **Causality Queries** (NEW)
```clojure
;; Temporal regression hunting: what change broke this test?
'[:find ?root-event ?cascade-chain
  :in $ ?failing-test
  :where
  [?test-event :event/type :test-run]
  [?test-event :event/target ?failing-test]
  [?test-event :event/timestamp ?test-time]
  
  ;; Find changes that affected dependencies before test
  [?change :event/affects ?dependency]
  [?dependency :var/used-by ?failing-test]
  [?change :event/timestamp ?change-time]
  [(< ?change-time ?test-time)]
  
  ;; Trace back to root cause
  (or [?change :event/triggered-by ?root-event]
      [?change ?root-event])
  
  ;; Build causality chain
  [(path ?root-event ?change) ?cascade-chain]]
```

#### 3. **True Dependencies** (FQN Usage vs Requires)
```clojure
;; What does my.app actually depend on? (not just require)
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

#### 4. **Blast Radius Estimation** (NEW)
```clojure
;; Quantify refactoring risk
'[:find ?var ?risk-score
  :in $ ?target-var
  :where
  ;; Find all dependents
  [?usage :usage/callee ?target-var]
  [?usage :usage/caller ?dependent]
  
  ;; Calculate risk based on change velocity
  [?event :event/target ?dependent]
  [?event :event/timestamp ?when]
  [(recent-changes ?dependent) ?change-velocity]
  
  ;; Risk = dependents × change velocity × depth
  [(* (count ?dependent) ?change-velocity) ?risk-score]]

;; Result with risk assessment
{:blast-radius
 {:direct-dependents 12
  :transitive-dependents 147
  :high-velocity-dependents 8  ; Changed >5 times in last week
  :risk-score 0.76             ; 0.0 = safe, 1.0 = dangerous
  :recommendation "Focus testing on high-velocity dependents"}}
```

## Schema Views: Multiple Lenses on the Same Graph

**Feedback Consensus**: Provide both low-level entity graph AND high-level semantic views.

```clojure
;; Define pluggable views for different user personas
(defview dependency-view :var
  :query '[:find ?dep :where [?var :var/uses ?dep]]
  :render render-dependency-graph)

(defview temporal-view :var
  :query '[:find ?event :where [?event :event/target ?var]]
  :render render-timeline)

(defview architectural-layer-view :namespace
  :query '[:find ?ns ?layer 
           :where 
           [?ns :ns/name ?name]
           [(layer-from-name ?name) ?layer]]
  :render render-c4-diagram)

(defview component-view :namespace
  :query '[:find ?component ?namespaces
           :where
           [?ns :ns/name ?name]
           [(deployment-unit ?name) ?component]
           [(group-by ?component ?ns) ?namespaces]]
  :render render-component-diagram)

;; Users can switch views based on their task
;; Same entity graph, different perspectives
```

## File & Statement Analysis: The REPL-vs-File Problem

### The Core Challenge

**Files represent ordered, deterministic evaluation**. **REPL sessions are exploratory and out-of-order**. This fundamental tension creates bugs that appear when code is reloaded.

### Critical Query Patterns

#### 1. **File-vs-Runtime Divergence Detection**
```clojure
;; Find namespaces where runtime state differs from file order
'[:find ?ns ?file-order ?actual-order ?divergence
  :where
  [?file :file/namespace ?ns]
  [?file :file/eval-order ?file-order]
  
  ;; Get actual eval order from events
  [?event :event/type :statement-eval]
  [?event :event/statement ?stmt]
  [?stmt :statement/namespace ?ns]
  [(actual-eval-sequence ?ns) ?actual-order]
  
  [(order-divergence ?file-order ?actual-order) ?divergence]
  [(pos? ?divergence)]]

;; Result with UI warning:
{:namespace 'my.app
 :divergence-count 2
 :warnings
 [{:type :out-of-order-eval
   :file-position 2
   :actual-position 1
   :statement "(defn process ...)"
   :risk :high
   :message "Function evaled BEFORE its dependency 'config'"}
  {:type :never-evaled
   :file-position 3
   :statement "(defn helper ...)"
   :risk :medium
   :message "File contains helper fn but never evaled in REPL"}]}
```

#### 2. **Side-Effect Detection**
```clojure
;; Find statements with side effects that were evaled out of order
'[:find ?stmt ?expected-pos ?actual-pos ?side-effects
  :where
  [?stmt :statement/side-effects ?effects]
  [(seq ?effects) true]  ; Has side effects
  
  [?stmt :statement/position ?expected-pos]
  [?event :event/statement ?stmt]
  [?event :event/sequence-id ?actual-seq]
  [(position-from-sequence ?actual-seq) ?actual-pos]
  
  [(not= ?expected-pos ?actual-pos)]
  [(= ?side-effects ?effects)]]

;; UI Display:
⚠️  Side-effectful statements evaled out of order:

1. (def config (read-config))     Expected: pos 1, Actual: pos 3
   Side effects: [:io]
   Risk: Config read AFTER process fn defined - process may use stale data

2. (alter-var-root #'config ...)  Expected: pos 3, Actual: pos 2
   Side effects: [:var-mutation]
   Risk: Config mutated before process fn defined
```

#### 3. **Missing Forward Declarations**
```clojure
;; Find vars that could benefit from forward declarations
'[:find ?stmt ?missing-declare ?used-before-defined
  :where
  [?stmt :statement/defines ?var]
  [?stmt :statement/position ?def-pos]
  
  ;; Find statements that use this var
  [?use-stmt :statement/depends-on ?var]
  [?use-stmt :statement/position ?use-pos]
  
  ;; Used before defined
  [(< ?use-pos ?def-pos)]
  
  ;; No declare exists
  (not [?decl :statement/type :declare]
       [?decl :statement/defines ?var])
  
  [(= ?missing-declare ?var)]
  [(= ?used-before-defined ?use-stmt)]]

;; Recommendation:
Add forward declarations to enable safe reordering:

(declare process helper validate)

This allows REPL experimentation without order constraints.
```

#### 4. **Reorder Safety Analysis**
```clojure
;; Which statements can be safely reordered?
'[:find ?stmt ?reorder-safe?
  :where
  [?stmt :statement/type ?type]
  
  ;; Check purity
  [?stmt :statement/purity ?purity]
  [?stmt :statement/side-effects ?effects]
  
  ;; Check if all dependencies could be forward-declared
  [?stmt :statement/depends-on ?deps]
  [(all-declarable? ?deps) ?all-decl]
  
  ;; Safe if pure OR pure-with-declares AND no side effects
  [(or (= ?purity :pure)
       (and (= ?purity :pure-with-forward-declare)
            ?all-decl
            (empty? ?effects))) ?reorder-safe?]]
```

#### 5. **Session Reproducibility Check**
```clojure
;; Can I reproduce my current REPL state by reloading the file?
'[:find ?ns ?reproducible? ?blocking-issues
  :where
  [?file :file/namespace ?ns]
  [?file :file/eval-order ?file-order]
  
  ;; Get REPL eval events
  [(repl-eval-sequence ?ns) ?repl-order]
  
  ;; Check if reloading file would produce same state
  [(order-compatible? ?file-order ?repl-order) ?compatible]
  [(find-blocking-issues ?file-order ?repl-order) ?issues]
  
  [(and ?compatible (empty? ?issues)) ?reproducible?]
  [(= ?blocking-issues ?issues)]]

;; Result:
{:namespace 'my.app
 :reproducible? false
 :blocking-issues
 [{:type :out-of-order-dependency
   :statement "(defn process [x] (helper x))"
   :problem "Calls helper before helper is defined in file"
   :fix "Add (declare helper) before process"}
  {:type :missing-side-effect
   :statement "(alter-var-root #'config assoc :timeout 5000)"
   :problem "REPL has mutated config, but file doesn't reflect this"
   :fix "Update file to match REPL state or remove mutation"}]}
```

### UI Patterns for File/Statement Issues

**Real-time Warning in Editor**:
```
my/app.clj [⚠️ Runtime divergence detected]

Line 15: (defn process [x] (helper x))
         ⚠️  Calls 'helper' before it's defined (line 20)
         💡 Add: (declare helper) at line 10
         🔴 REPL evaled this BEFORE helper was defined - may fail on reload

Line 20: (defn helper [x] (* x 2))
         ✅ Definition matches REPL state
         
Line 25: (alter-var-root #'config assoc :timeout 5000)
         ⚠️  REPL skipped this statement
         🔴 File config differs from runtime config
```

**Pre-Reload Sanity Check**:
```
About to reload my.app namespace...

Checking for issues:
✅ No missing forward declarations
⚠️  3 statements have side effects
   → (def config (read-config))  [re-runs I/O]
   → (defonce db (create-db))    [won't re-run due to defonce]
   → (println "Loading...")      [will print again]

❌ 2 out-of-order dependencies detected
   → process uses helper before helper defined

Recommendation: Fix order issues before reloading
```

## Enhanced Use Cases (Feedback-Informed)

### 1. **Development Workflow**

| Use Case | Query Type | Value |
|----------|------------|-------|
| **Confidence-Driven Development** | Staleness-aware | Pre-commit: show all stale entities touched by my changes |
| **Temporal Regression Hunting** | Causality chain | Given failing test, find root cause change via event correlation |
| **Blast Radius Estimation** | Impact + velocity | Quantify refactoring risk: dependents × change rate |
| **Pre-commit Sanity Check** | Confidence threshold | Block commit if critical analyses have confidence < 0.8 |
| **File-vs-REPL Divergence** (NEW) | Statement order | Detect when REPL state won't reproduce from file reload |
| **Reorder Safety Analysis** (NEW) | Purity + dependencies | Identify statements safe to reorder with forward declares |

### 2. **Architecture & Quality**

| Use Case | Query Type | Value |
|----------|------------|-------|
| **Codebase Cartography** | Centrality metrics | Visual map: "continents" (high-centrality namespaces), "cities" (frequently-used vars) |
| **Architectural Drift Detection** | Rule validation | Define architecture rules as queries; fail CI if violated |
| **Boundary Contract Monitoring** | API stability | Compare public API change velocity vs. advertised stability |
| **Module Cohesion Analysis** | Coupling ratio | Internal vs. external coupling to identify refactoring candidates |

### 3. **Knowledge & Learning**

| Use Case | Query Type | Value |
|----------|------------|-------|
| **Onboarding Paths** | Dependency depth | Generate ordered reading list: "read these 20 functions in this order" |
| **Knowledge Gap Analysis** | Author + time | Find critical code not touched by active team members (bus factor) |
| **Living Architecture Diagrams** | Namespace layers | Auto-generate C4 diagrams from entity graph, update real-time |

### 4. **AI-Augmented Analysis** (2025 Opportunity)

| Use Case | Integration | Value |
|----------|-------------|-------|
| **LLM-Powered Codebase Q&A** | Entity graph → LLM context | "Why does auth fail?" → structured entities + LLM reasoning |
| **AI-Assisted Impact Analysis** | Graph queries + semantics | "What breaks if I make this async?" → entities + LLM behavior analysis |

## Temporal Strategies: Working with Imperfect Data

### Staleness Budgets by Use Case

```clojure
(def staleness-thresholds
  {:safety-critical-analysis  1000   ; 1s max for refactoring safety checks
   :general-exploration       5000   ; 5s OK for browsing  
   :historical-trends         30000  ; 30s fine for change patterns
   :architecture-overview     300000}) ; 5m acceptable for big picture
```

### Confidence Propagation Through Graph

```clojure
(defn query-with-confidence [query]
  (let [results (d/q query @db)
        entity-confidences (map :entity/confidence results)
        min-confidence (apply min entity-confidences)]
    {:results results
     :meta {:confidence min-confidence
            :distribution (confidence-histogram entity-confidences)
            :weak-links (filter #(< (:entity/confidence %) 0.5) results)}}))
```

### Visual Staleness Indicators (UI Pattern)

- 🟢 **Fresh** (< 1s, confidence > 0.9): Full trust in results
- 🟡 **Recent** (1-5s, confidence 0.7-0.9): Probably accurate, show age
- 🟠 **Stale** (5-30s, confidence 0.4-0.7): Likely outdated, warn user
- 🔴 **Ancient** (>30s, confidence < 0.4): Definitely wrong, suggest re-analysis

## Implementation Roadmap (Feedback Consensus)

### Phase 0: The One-Liner (Week 1) - **CRITICAL FIRST STEP**

**Goal**: Prove concept with minimal commitment.

```clojure
;; Single-function library that just works:
(require '[clj-surveyor.core :as survey])
(survey/dependents 'my.app/process-user)
;; => Prints dependency tree with confidence indicators
```

**Success Criteria**: 5-10 early adopters using regularly.

### Phase 1: CLI Tool (Months 1-2)

**Goal**: Validate entity model and query patterns.

**Features**:
- Command-line tool: `surveyor analyze`, `surveyor query "..."`
- Interactive query REPL
- Pre-built queries for common tasks
- ASCII visualizations with staleness indicators
- Integration: Bootstrap from clj-kondo analysis + runtime introspection

**Success Criteria**:
- Query response < 100ms for typical codebase
- Memory usage < 50MB
- 10+ developers using regularly

### Phase 2: Editor Integration (Months 3-4)

**Goal**: Zero-friction, in-flow usage.

**Features**:
- nREPL middleware for change event capture
- Emacs/VSCode/IntelliJ plugins
- Inline hover: "This var has 47 dependents (confidence: 0.85)"
- Refactoring hints: "⚠️ 12 high-velocity dependents detected"
- Pre-commit checks: staleness warnings

### Phase 3: Web Dashboard (Months 5-6)

**Goal**: Team collaboration and architectural overview.

**Features**:
- Real-time entity graph visualization
- Multiple views (dependency, temporal, architectural, component)
- Confidence-driven UI: color-code by staleness
- Team features: shared queries, annotations (intent entities)
- WebSocket API: reactive Datalog queries

### Phase 4: Advanced Features (Months 7-12)

**Features**:
- Incremental computation for performance
- Intent capture UI (human-in-the-loop annotations)
- Architectural rule enforcement in CI/CD
- LLM integration for AI-powered analysis
- Historical replay: "show me entity graph state at commit abc123"

## Enhanced Schema Design

```clojure
(def schema-v3
  {;; Core entities
   :entity/fqn      {:db/unique :db.unique/identity :db/index true}
   :entity/type     {:db/index true}
   
   ;; Temporal tracking
   :entity/captured-at    {:db/index true}
   :entity/confidence     {:db/index true}
   :entity/staleness-ms   {:db/index true}
   :entity/version        {:db/index true}
   
   ;; Composite indexes for common queries (performance optimization)
   :entity/type+confidence {:db/index true}  ; "find high-confidence vars"
   
   ;; Var relationships
   :var/uses        {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many}
   :var/used-by     {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many}
   
   ;; Usage tracking (frequency analysis)
   :usage/caller    {:db/valueType :db.type/ref}
   :usage/callee    {:db/valueType :db.type/ref}
   :usage/count     {:db/index true}           ; hot-path analysis
   :usage/frequency-hz {:db/index true}        ; calls per second
   
   ;; Event entities (expanded taxonomy)
   :event/type      {:db/index true}
   :event/target    {:db/valueType :db.type/ref :db/index true}
   :event/timestamp {:db/index true}
   :event/triggered-by {:db/valueType :db.type/ref}  ; causality
   :event/cascade-depth {:db/index true}              ; nth-order effects
   :event/sequence-id   {:db/index true}              ; temporal ordering
   
   ;; Intent entities (human context)
   :intent/author   {:db/index true}
   :intent/affected-entities {:db/valueType :db.type/ref
                              :db/cardinality :db.cardinality/many}
   :intent/status   {:db/index true}
   
   ;; File entities (NEW: eval order tracking)
   :file/path       {:db/unique :db.unique/identity :db/index true}
   :file/namespace  {:db/valueType :db.type/ref}
   :file/statements {:db/valueType :db.type/ref
                     :db/cardinality :db.cardinality/many
                     :db/isComponent true}  ; Owned by file
   :file/eval-order {:db/cardinality :db.cardinality/many}  ; Ordered list
   :file/last-modified {:db/index true}
   :file/last-evaled   {:db/index true}
   :file/partial-eval? {:db/index true}
   
   ;; Statement entities (NEW: top-level forms)
   :statement/id    {:db/unique :db.unique/identity}
   :statement/file  {:db/valueType :db.type/ref}
   :statement/namespace {:db/valueType :db.type/ref}
   :statement/type  {:db/index true}  ; :defn, :def, :alter-var-root, etc.
   :statement/defines {:db/valueType :db.type/ref}  ; var it creates
   :statement/depends-on {:db/valueType :db.type/ref
                          :db/cardinality :db.cardinality/many}
   :statement/side-effects {:db/cardinality :db.cardinality/many}  ; set of keywords
   :statement/purity {:db/index true}  ; :pure, :pure-with-forward-declare, :impure
   :statement/position {:db/index true}  ; position in file
   :statement/must-follow {:db/valueType :db.type/ref
                           :db/cardinality :db.cardinality/many}
   :statement/can-reorder? {:db/index true}
   :statement/last-evaled  {:db/index true}
   :statement/eval-count   {:db/index true}
   :statement/eval-source  {:db/index true}  ; :repl-individual, :file-load
   
   ;; Staleness tracking
   :staleness/scope {:db/valueType :db.type/ref}
   :staleness/confidence {:db/index true}})
```

## Change Detection Implementation

```clojure
(ns clj-surveyor.capture
  (:require [clojure.core :as core]))

;; Hook into var redefinition
(defn watch-namespace [ns-sym]
  (doseq [var-sym (keys (ns-interns ns-sym))]
    (let [var (ns-resolve ns-sym var-sym)]
      (add-watch var ::surveyor
        (fn [key ref old-val new-val]
          (record-event!
            {:type :var-redefinition
             :target (symbol (str ns-sym) (str var-sym))
             :timestamp (System/currentTimeMillis)
             :detection-lag (measure-lag)
             :source (eval-source)})))))

;; Batch change events to reduce overhead
(defonce event-buffer (atom []))

(defn record-event! [event]
  (swap! event-buffer conj event)
  (when (or (> (count @event-buffer) 100)           ; batch size
            (> (event-age (first @event-buffer)) 100)) ; max delay 100ms
    (flush-events!)))

;; Async event processing (don't block REPL)
(defn flush-events! []
  (future
    (let [events @event-buffer]
      (reset! event-buffer [])
      (process-event-batch! events))))
```

## Performance & Scalability

### Tiered Storage Strategy

```clojure
;; Hot entities (recent, frequently accessed): In-memory Datascript
;; Warm entities (occasional access): Disk-backed Datahike
;; Cold entities (historical): Compressed append-log

(defprotocol EntityStorage
  (store-entity [this entity])
  (retrieve-entity [this fqn])
  (query-entities [this query]))

(defrecord TieredStorage [hot warm cold]
  EntityStorage
  (store-entity [this entity]
    (let [tier (classify-tier entity)]
      (case tier
        :hot  (d/transact! hot [entity])
        :warm (datahike/transact! warm [entity])
        :cold (append-to-log! cold entity))))
  
  (query-entities [this query]
    ;; Try hot tier first (fastest)
    (or (d/q query @hot)
        ;; Fallback to warm tier
        (datahike/q query @warm)
        ;; Last resort: load from cold storage
        (query-cold-storage cold query))))
```

### Query Performance Optimizations

```clojure
;; Result caching with staleness-aware TTL
(defn cached-query [query staleness-budget]
  (let [cache-key (hash query)
        cached (get @query-cache cache-key)]
    (if (and cached 
             (< (- (now) (:timestamp cached)) staleness-budget))
      (:result cached)
      (let [result (execute-query query)]
        (swap! query-cache assoc cache-key 
               {:result result :timestamp (now)})
        result))))
```

## Integration Strategy: Complement, Don't Replace

| Existing Tool | clj-surveyor Integration | Value Add |
|---------------|--------------------------|-----------|
| **clj-kondo** | Bootstrap entity graph from static analysis | Runtime validation of static hints |
| **clojure-lsp** | Provide entity graph via LSP extensions | Historical context + relationship queries |
| **tools.namespace** | Seed initial namespace entities | Temporal evolution tracking |
| **CIDER/Calva** | nREPL middleware for seamless integration | No workflow disruption |
| **clj-ns-browser** | Compatibility bridge: feed it our entities | Smooth migration path |

## Success Metrics (Measurable Goals)

1. **Performance**:
   - Query response time: <100ms for 95th percentile
   - Memory overhead: <50MB for medium projects (10k vars)
   - REPL impact: <5ms additional latency per eval

2. **Coverage**:
   - Entity capture: 99%+ of runtime var definitions
   - Change detection: <50ms average lag
   - Confidence: >0.8 average for active code

3. **Adoption**:
   - Phase 0: 10 early adopters in first month
   - Phase 1: 100 active CLI users by month 3
   - Phase 2: 500 editor integration users by month 6

4. **Impact**:
   - Reduce time-to-understand-codebase by 50%+
   - Catch 80%+ of runtime-vs-file mismatches before bugs
   - Prevent 10+ architectural violations per month (via CI checks)

## Differentiation Matrix

| Dimension | Static Tools | clj-surveyor |
|-----------|--------------|--------------|
| **Data Source** | Files & AST | Runtime + Files |
| **Temporal Awareness** | None | First-class concern |
| **Confidence Tracking** | Assumed perfect | Explicit & propagated |
| **Dependency Model** | `require` statements | FQN usage (true deps) |
| **Queryability** | Fixed analyses | Arbitrary Datalog queries |
| **Historical Context** | Git only | Event streams + causality |
| **User Personas** | One size fits all | Multiple views/lenses |
| **AI Integration** | Not designed for it | LLM-friendly from day one |

**Unique Value Proposition**: clj-surveyor is the only tool that combines runtime-first entity modeling, temporal awareness with confidence tracking, queryable relationship graphs, and **file-vs-REPL divergence detection**—making it ideal for both human developers and AI-augmented workflows.

---

## Key Innovations Summary

### 1. **Runtime-First with Temporal Honesty**
Most tools pretend perfect knowledge. clj-surveyor acknowledges imperfection, tracks staleness, and propagates confidence through every query.

### 2. **FQN-Based True Dependencies**
Distinguishes between `require` (convenience) and actual symbol usage (true dependencies), enabling accurate impact analysis.

### 3. **Event-Driven Causality Tracking**
Not just "what changed" but "what triggered this cascade of changes"—critical for temporal regression hunting.

### 4. **File-as-Sequence Model** (NEW)
Recognizes that files are ordered evaluations with side effects. Tracks eval order, detects REPL-vs-file divergence, identifies when "works in my REPL" won't survive reload.

### 5. **Multiple Lenses on Same Graph**
Different views (dependency, temporal, architectural, component) for different user personas—same entity data, different perspectives.

### 6. **AI-Augmented Ready**
Entity serialization designed for LLM consumption, positioning clj-surveyor as the "code understanding layer" for AI-powered development tools.

---

## Next Actions (Immediate)

### Before Coding (Validation Phase)

1. **Build Community**:
   - [ ] Post RFC in Clojure forums, Slack, ClojureVerse
   - [ ] Get feedback from clj-kondo, clojure-lsp maintainers
   - [ ] Recruit 5-10 early adopters for Phase 0 testing

2. **Validate Assumptions**:
   - [ ] Benchmark `add-watch` overhead on real REPLs
   - [ ] Test Datascript with 100k entities on typical hardware
   - [ ] Survey: will developers accept "stale data" with confidence indicators?

3. **Define Baselines**:
   - [ ] Measure current time-to-understand-codebase (surveys)
   - [ ] Document pain points with existing tools
   - [ ] Set concrete success metrics

### Week 1: Phase 0 Implementation

```clojure
;; Goal: Single function that works in any project
(ns clj-surveyor.core
  (:require [datascript.core :as d]))

(defn dependents 
  "Show what depends on the given var symbol.
   Returns tree with confidence indicators."
  [var-sym]
  (let [graph (build-entity-graph)  ; scan runtime
        deps (query-dependents graph var-sym)]
    (render-tree deps :show-confidence true)))

;; Usage:
;; (require '[clj-surveyor.core :as survey])
;; (survey/dependents 'my.app/process-user)
```

**Success = 10 developers say "this is useful"**

---

*clj-surveyor v3: From concept to concrete implementation plan, grounded in community feedback and proven patterns.*
