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
 
 ;; Persistence tracking (NEW)
 :var/file-backed?  true                    ; Defined in a file?
 :var/ephemeral?    false                   ; REPL-only definition?
 :var/file-diverged? false                  ; REPL value differs from file?
 
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

**Critical Insight**: File loading and statement evaluation are both **event sequences**. Loading a file is just a batch eval of ordered statements. REPL eval is granular, often out-of-order. Every eval event potentially invalidates dependents, but determining **which dependents need re-eval** and **how far the cascade should go** is the fundamental challenge.

```clojure
{:entity/type :event
 :event/type  #{:file-load           ; Batch eval of all statements in order
                :statement-eval       ; Single statement eval (REPL)
                :namespace-reload     ; Explicit reload (may skip defonce)
                :var-redefinition     ; def/defn overwrites existing var
                :test-run             ; Test execution
                :require-invoked      ; Dependency loaded
                :profiling-sample     ; Performance data
                :repl-command}        ; User interaction
 
 :event/target      'my.app/process-user  ; What was affected
 :event/timestamp   #inst "2024-10-29T15:30:00.456Z"
 :event/source      :repl-eval | :file-load | :ns-reload
 
 ;; Persistence tracking (NEW)
 :event/persistent? true                    ; Saved to file?
 :event/file-backed "src/my/app.clj"        ; If persistent
 :event/ephemeral?  false                   ; REPL-only, will revert on restart
 
 ;; For file-load events (NEW)
 :event/file        "src/my/app.clj"
 :event/statements  [{:id 1} {:id 2} {:id 3}]  ; Ordered sequence evaled
 :event/eval-order  [1 2 3]                    ; Canonical file order
 
 ;; For statement-eval events (NEW)
 :event/statement   {:db/valueType :db.type/ref}  ; Which statement
 :event/position    2                              ; Where in file
 :event/out-of-order? true                        ; Evaled before dependencies?
 
 ;; Impact tracking (NEW)
 :event/affected-vars       [{:db/valueType :db.type/ref}]  ; Direct impact
 :event/stale-dependents    23                              ; Vars now stale
 :event/cascade-needed?     true                            ; Should propagate?
 :event/cascade-confidence  0.7                             ; How sure?
 :event/cascade-depth       3                               ; How far to go?
 
 ;; Causality tracking
 :event/triggered-by    {:db/valueType :db.type/ref}  ; parent event
 :event/cascade-from    {:db/valueType :db.type/ref}  ; original change
 :event/causality-confidence 0.8
 
 ;; Temporal context
 :event/detection-lag   23      ; ms between actual change and detection
 :event/sequence-id     1247}   ; order in event stream
```

**The Cascade Dilemma**:

When you eval `(def config {:timeout 60})` and it has 23 transitive dependents, **do all 23 need re-eval?** The honest answer: **we can't know for certain**.

- **Best case**: Config change doesn't semantically affect dependents (e.g., just a timeout value)
- **Worst case**: Every dependent's behavior changed, all need re-eval and re-test
- **Reality**: Somewhere in between, depends on actual code semantics

**clj-surveyor's approach**: Track the cascade potential, show the user, let them decide—but provide confidence indicators to guide the decision.

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
 :statement/purity :pure | :pure-isolated | :impure-var-with-deps | :impure-known
 :statement/downstream-impact 23  ; Number of vars affected if this statement changes
 
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
  #{:io                 ; Reads/writes external data
    :var-mutation       ; alter-var-root, def, defonce
    :state-change       ; swap!, reset!, alter, etc.
    :resource-creation  ; database connections, file handles
    :require}           ; Loads other namespaces
```

**Purity Levels** (Dependency-Aware):

The critical insight: **var assignment is only "pure" if nothing depends on that var**. Otherwise, the assignment is a side effect that propagates through the entire dependency graph.

- **`:pure-isolated`** - Pure computation with no var assignment
  - `(comment (+ 1 2 3))` - REPL experiments
  - `(assert (= 2 (+ 1 1)))` - Inline tests
  - No state mutation, safe to eval any time
  - **Confidence**: HIGH (static analysis)

- **`:pure`** - Var definition with **zero dependents**
  - `(def config 42)` - nothing uses it yet
  - `(defn helper [x] (* x 2))` - not called anywhere
  - Safe to reorder because no downstream impact
  - **Confidence**: HIGH (query dependency graph)

- **`:pure-with-forward-declare`** - Pure after forward declarations added
  - `(defn process [x] (helper x))` where helper defined later
  - Needs `(declare helper)` to enable reordering
  - **Confidence**: HIGH (static analysis + graph)

- **`:impure-var-with-deps`** - Var assignment affecting dependents
  - `(def foo 42)` where other code uses `foo`
  - `(defn bar [x] ...)` redefining function in active use
  - **Impact**: All dependents now stale, REPL diverged from file
  - **Side Effect**: Mutation propagates through dependency graph
  - **Confidence**: HIGH (traverse dependency graph for dependents)

- **`:impure-var-unknown-deps`** - Var assignment, dependencies unclear
  - `(def foo 42)` but dependency analysis incomplete
  - Assume impure until proven otherwise (safe default)
  - **Confidence**: MEDIUM (incomplete graph)

- **`:impure-known`** - I/O, state mutation, resource creation
  - `(println "Loading...")` - I/O side effect
  - `(reset! atom-val 42)` - State mutation
  - `(create-database-connection)` - Resource creation
  - **Confidence**: HIGH (static analysis detects known patterns)

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

### The Dependency-Aware Side-Effect Model

**Critical Insight**: A var assignment like `(def foo 42)` isn't inherently "pure" just because it doesn't do I/O. **If any other code depends on that var, the assignment is a side effect that propagates through the entire dependency graph**.

**The Cascade Problem**: When you redefine a var with dependents, **how many need re-eval?** And **how do we know?** This is the fundamental uncertainty of REPL-driven development.

#### The Miracle of Working Software

It's actually remarkable that our software works at all. Consider:

1. **File load**: Eval 50 statements in order → 50 vars defined
2. **REPL experiment**: Eval statement #30 in isolation → 1 var redefined
3. **Cascade question**: Which of the other 49 statements are now invalid?

**We can't know with certainty**. We can:
- Track **syntactic dependencies** (what calls what)
- Detect **temporal staleness** (what changed when)
- Estimate **impact radius** (how many affected)
- Classify **purity levels** (side-effect potential)

But **semantic equivalence**? ("Does this change actually matter?") That requires understanding code meaning, which is AI-hard.

**The Deeper Problem: Runtime Call Tracking**

Even if we know `function-b` depends on `var-a`, we face an even harder problem:

```clojure
(def config {:timeout 30})

(defn process [x]
  (with-timeout (:timeout config)  ; Closes over config's VALUE
    (validate x)))

;; Later in REPL:
(def config {:timeout 60})         ; Redefine config

;; Question: Does process need re-eval?
;; Answer: Depends on WHEN it was called!
```

**The intractable questions**:
1. **Was `process` ever called?** (If not, re-eval doesn't matter yet)
2. **When was it last called?** (Before or after config changed?)
3. **By whom?** (Which caller invoked it? With what args?)
4. **Did the call capture the old or new config value?**
5. **Does it even matter?** (If timeout change doesn't affect behavior)
6. **Did the value change cause different code paths?** (Conditional branching we can't predict)

**The Branching Problem**:

Changing a var's value can cause the entire application to take a completely different execution path:

```clojure
(def feature-flags {:new-algorithm-enabled? false})

(defn process [data]
  (if (:new-algorithm-enabled? feature-flags)
    (new-fast-algorithm data)      ; Path A: never executed before
    (old-slow-algorithm data)))    ; Path B: always executed

;; Later in REPL:
(def feature-flags {:new-algorithm-enabled? true})

;; Now process will call new-fast-algorithm
;; - Which may not even be loaded yet
;; - Which may have different dependencies
;; - Which may behave completely differently
;; - Which creates a whole new dependency subgraph we haven't analyzed
```

**We can't predict**:
- Which code paths will execute with new values
- What dependencies those paths will need
- Whether those dependencies are even loaded/defined
- What side effects the new paths will trigger
- How control flow changes ripple through the system

Even with perfect call tracking, we'd need **symbolic execution** or **runtime profiling** to know which branches are taken with different values.

**clj-surveyor cannot track runtime invocations or control flow**. We can't know:
- Which functions were actually called
- When they were called relative to var changes
- What values they captured from the environment
- Whether stale closures are still alive in the runtime
- **Which code paths will execute with changed values**
- **What new dependencies emerge from conditional branches**

This would require **invasive runtime instrumentation** (every function call logged, every branch tracked) with massive performance overhead and memory requirements.

**What we CAN do (static analysis)**:
- ✅ Track var definitions and redefinitions (via `add-watch`)
- ✅ Detect syntactic dependencies (what symbols appear in function bodies)
- ✅ Build dependency graph (who calls whom - static structure)
- ✅ Timestamp changes to detect staleness windows
- ✅ Classify purity to estimate side-effect scope
- ✅ Identify potential branching points (if/case/cond forms)

**What we CANNOT do (without runtime tracing)**:
- ❌ Know if a function was called after a var change
- ❌ Track which values were captured in closures
- ❌ Detect stale references in long-lived objects
- ❌ Trace call chains through the runtime
- ❌ Determine if staleness actually affects behavior
- ❌ **Predict which code paths will execute with new values**
- ❌ **Know what dependencies emerge from conditional branches**
- ❌ **Detect when value changes cause control flow shifts**
- ❌ **Track application state** (web servers, connections, caches, atoms, refs)
- ❌ **Find stale closures in running systems** (returned from previous calls)

**clj-surveyor's honest approach**:
- Show the dependency graph (static structure)
- Flag **potential** staleness (temporal windows)
- Provide confidence indicators based on what we **can** know
- Identify branching points where control flow depends on var values
- **Acknowledge uncertainty** - we show possibilities, not guarantees
- **Surface the unknowable** - make explicit what we can't determine
- **Let the human decide** based on domain knowledge

**The Layered State Problem**:

A running Clojure system has multiple layers of state, each requiring different refresh strategies:

| State Layer | What Lives Here | Refresh Strategy | Staleness Risk |
|-------------|----------------|------------------|----------------|
| **File-Backed Definitions** | Saved .clj files | Reload from disk | ✅ Persistent, survives restart |
| **REPL-Only Definitions** | Ad-hoc evals not saved | Re-eval manually | ⚠️ Ephemeral, lost on restart |
| **Namespace State** | requires, imports | Reload namespace | ✅ clj-surveyor can detect |
| **Application State** | Web servers, DB pools | Restart app or re-init | ❌ Can't detect from definitions |
| **Runtime Objects** | Closures, cached values | Recreate objects | ❌ Invisible to static analysis |
| **External Systems** | Databases, files, queues | Re-initialize | ❌ Outside Clojure entirely |

**Critical Distinction: Persistent vs. Ephemeral Changes**

1. **Persistent changes** (saved to files):
   - Survive application restart
   - Reflected in version control
   - Affect all future sessions
   - Example: `(defn process [x] ...)` saved in `my/app.clj`

2. **Ephemeral changes** (REPL-only evals):
   - Lost on REPL restart
   - Not in version control
   - Experimental, temporary
   - Example: `(def config {:timeout 9999})` evaled but not saved
   
**The Divergence Problem**:

When you have both persistent (file) and ephemeral (REPL-only) changes, you have **three different states**:

```clojure
;; State 1: FILE (on disk)
(def config {:timeout 30})

;; State 2: REPL (in memory, after experimenting)
(def config {:timeout 9999})  ; Changed but not saved!

;; State 3: APP (after restart)
(def config {:timeout 30})    ; Reverts to file state, ephemeral change lost
```

**clj-surveyor's approach**:

- Track **both** file-backed and REPL-only definitions
- Flag divergence: "config in REPL differs from file"
- Mark changes as `:persistent` or `:ephemeral` based on file presence
- Warn: "Ephemeral change will be lost on restart unless saved to file"

**Refresh strategies by change type**:

| Change Type | Where | Refresh Method | Persists After Restart? |
|-------------|-------|----------------|------------------------|
| Saved file edit | Disk | Reload file | ✅ Yes |
| REPL ad-hoc eval | Memory | Re-query/re-eval | ❌ No - reverts to file |
| REPL + saved | Both | Already persistent | ✅ Yes |

**REPL doesn't need restart** - it just needs to re-query for changed values and update its dependency analysis. But developers need to know which changes are ephemeral vs. persistent.

Because sometimes you *know* the change is safe (just a docstring). Sometimes you know it's dangerous (core algorithm). Sometimes you know which code path will execute (feature flag you control). And sometimes you know the function hasn't been called yet, so staleness doesn't matter.

The tool provides the best evidence we can gather without runtime tracing. The developer makes the call using knowledge the tool can't have.

**Example: Feature Flag Awareness** (Potential Enhancement)

```clojure
;; We could detect conditional dependencies statically:
(defn analyze-conditional-deps [fn-source]
  {:static-deps     #{old-slow-algorithm}  ; Always referenced syntactically
   :conditional-deps {true  #{new-fast-algorithm}   ; Only if flag true
                      false #{old-slow-algorithm}}  ; Only if flag false
   :control-var     'feature-flags
   :warning "Function behavior depends on feature-flags value.
             Changing feature-flags will alter code path at runtime.
             We cannot predict which path will execute."})
```

This gives developers a heads-up that changing `feature-flags` has **unpredictable impact** beyond the static dependency graph.

#### Why Var Assignment = Side Effect (When It Has Dependents)

```clojure
;; File: my/app.clj

(def config {:timeout 30})        ; Line 1: Initial definition

(defn process-user [user]         ; Line 2: Uses config
  (with-timeout (:timeout config)
    (validate user)))

(defn validate [user]              ; Line 3: Uses process-user
  (process-user user))

;; Later in REPL...
(def config {:timeout 60})        ; Redefine config
```

**What just happened?**

1. `process-user` and `validate` were defined with `:timeout 30` in mind
2. Redefining `config` to `{:timeout 60}` **mutates** the value those functions see
3. Every function that depends on `config` (directly or transitively) is now **stale**
4. The REPL state diverged from the file

**This is a side effect** - you changed the behavior of downstream code without changing that code.

#### The Purity Hierarchy (Dependency-Aware)

Only var assignments with **zero dependents** are truly pure:

```clojure
;; TRULY PURE (zero dependents):
(def unused-helper [x] (* x 2))   ; Nothing calls this yet
;; → Safe to redefine, reorder, experiment with

;; IMPURE (has dependents):
(def config {:timeout 30})        ; process-user depends on this
;; → Redefining affects all dependents - side effect!

;; PURE-ISOLATED (no var assignment):
(comment (+ 1 2 3))               ; Just computation, no mutation
;; → Always safe, zero impact
```

#### Query Pattern: Detecting Dependent-Aware Purity

```clojure
;; For each statement, calculate downstream impact
'[:find ?stmt ?purity ?dependent-count ?transitive-count
  :in $ ?stmt-id
  :where
  [?stmt :statement/id ?stmt-id]
  [?stmt :statement/defines-var ?var]
  
  ;; Count direct dependents
  (or-join [?var ?direct-count]
    (and (not [?usage :usage/callee ?var])
         [(ground 0) ?direct-count])
    (and [?usage :usage/callee ?var]
         [(count-distinct ?usage) ?direct-count]))
  
  ;; Count transitive dependents
  [(transitive-dependent-count ?var) ?transitive-count]
  
  ;; Classify purity based on dependents
  (or-join [?direct-count ?transitive-count ?purity]
    ;; Zero dependents = pure
    (and [(= ?direct-count 0) true]
         [(ground :pure) ?purity])
    
    ;; Has dependents = impure (side effect on those dependents)
    (and [(> ?direct-count 0) true]
         [(ground :impure-var-with-deps) ?purity]))]

;; Result shows true impact:
{:statement "(def config {:timeout 30})"
 :purity :impure-var-with-deps
 :direct-dependents 5
 :transitive-dependents 23
 :side-effect-explanation
   "Redefining this var will change behavior of 23 downstream functions.
    This is a side effect even though no I/O occurs."}
```

### Critical Query Patterns

#### **Optional: Runtime Call Tracing** (Future Work, High Cost)

**If** we wanted to track actual function invocations (not just definitions), we could:

```clojure
;; Instrument functions to log calls
(defn instrument-var [var-sym]
  (let [original-fn @(resolve var-sym)]
    (alter-var-root (resolve var-sym)
      (fn [original]
        (fn [& args]
          (record-call! {:var var-sym
                         :timestamp (System/currentTimeMillis)
                         :args (str args)  ; Careful: could be huge/sensitive
                         :caller (current-stack-frame)})
          (apply original args)))
      (constantly original-fn))))

;; Query: Was function called after var changed?
'[:find ?fn ?last-call ?var-change ?is-stale
  :where
  [?var :var/fqn ?var-fqn]
  [?change-event :event/target ?var]
  [?change-event :event/timestamp ?var-change]
  
  ;; Find functions that depend on this var
  [?fn :var/uses ?var]
  
  ;; Find most recent call to function
  [?call-event :event/type :function-call]
  [?call-event :event/target ?fn]
  [?call-event :event/timestamp ?last-call]
  
  ;; Was call before or after var change?
  [(< ?last-call ?var-change) ?is-stale]]
```

**The Cost**:
- 📊 **Performance**: 10-100x overhead on every function call
- 💾 **Memory**: Potentially unbounded event log growth
- 🔒 **Privacy**: Logging args could capture sensitive data
- 🐛 **Complexity**: Instrumentation can break metaprogramming, stack traces
- 🚫 **Adoption barrier**: Most developers won't accept this overhead

**Verdict**: Not for Phase 1-3. Maybe Phase 4 as opt-in feature for specific debugging scenarios.

For now, clj-surveyor focuses on **zero-overhead static analysis** and lets runtime behavior remain opaque.

---

#### 1. **File-vs-Runtime Divergence Detection**

**Two types of divergence to detect**:

1. **Eval order divergence**: REPL evaled statements out of file order
2. **Value divergence**: REPL var value differs from file definition (ephemeral change)

```clojure
;; Detect ephemeral changes that will revert on restart
'[:find ?var ?repl-value ?file-value ?persistence
  :where
  ;; Var exists in runtime
  [?var :var/fqn ?fqn]
  [?var :var/source ?repl-value]
  
  ;; Var has a file definition
  [?var :var/file ?file-path]
  
  ;; Get current file content
  [(read-file-var ?file-path ?fqn) ?file-value]
  
  ;; Compare REPL vs file
  [(not= ?repl-value ?file-value)]
  
  ;; Classify persistence
  (or-join [?repl-value ?file-value ?persistence]
    ;; REPL matches file = persistent
    (and [(= ?repl-value ?file-value)]
         [(ground :persistent) ?persistence])
    
    ;; REPL differs from file = ephemeral
    (and [(not= ?repl-value ?file-value)]
         [(ground :ephemeral) ?persistence]))]

;; Result with warnings:
{:var 'my.app/config
 :repl-value "{:timeout 9999}"
 :file-value "{:timeout 30}"
 :persistence :ephemeral
 :warning
   "⚠️  EPHEMERAL CHANGE DETECTED
    
    REPL state: {:timeout 9999}
    File state: {:timeout 30}
    
    This change exists only in memory. On restart:
    - Application will revert to file value {:timeout 30}
    - Current REPL experiments will be lost
    - Any dependents using 9999 will break
    
    Actions:
    ✅ Save to file if this change should persist
    ⚠️  Document as experiment if temporary
    🔴 Restart will lose this change"}

;; Find all ephemeral changes
'[:find ?var
  :where
  [?var :var/ephemeral? true]]
```

**Eval Order Divergence** (existing pattern):

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

#### 2. **Cascade Decision Support** (NEW)

**The Core Question**: After an eval event, which dependents should be re-evaled?

**Critical Limitation**: We can detect **potential** staleness (syntactic dependencies + temporal analysis), but we **cannot know** if functions were actually called or if staleness matters in practice.

```clojure
;; Given an eval event, analyze cascade requirements
;; (Based on static dependencies + temporal analysis)
'[:find ?dependent ?cascade-priority ?confidence ?reasoning
  :in $ ?event-id
  :where
  [?event :event/id ?event-id]
  [?event :event/target ?changed-var]
  [?event :event/timestamp ?change-time]
  
  ;; Find all dependents (direct and transitive)
  [?usage :usage/callee ?changed-var]
  [?usage :usage/caller ?dependent]
  [?dependent :var/fqn ?dep-fqn]
  
  ;; Check if dependent was RE-EVALED (not called!) after the change
  ;; NOTE: We track definitions, not invocations
  (or-join [?dependent ?change-time ?is-stale]
    ;; No re-eval since change = POTENTIALLY stale
    (and (not [?dep-event :event/target ?dependent]
              [?dep-event :event/timestamp ?dep-time]
              [(> ?dep-time ?change-time)])
         [(ground true) ?is-stale])
    
    ;; Re-evaled after change = POTENTIALLY fresh
    ;; (But might still have stale closures if called before re-eval!)
    (and [?dep-event :event/target ?dependent]
         [?dep-event :event/timestamp ?dep-time]
         [(> ?dep-time ?change-time)]
         [(ground false) ?is-stale]))
  
  ;; Analyze the change type
  [?event :event/statement ?stmt]
  [?stmt :statement/purity ?purity]
  [?changed-var :var/arglists ?old-args]  ; From before change
  
  ;; Calculate cascade priority based on:
  ;; 1. Was dependent re-evaled? (not called, evaled!)
  ;; 2. What kind of change was it?
  ;; 3. How central is the dependent?
  [(cascade-priority ?is-stale ?purity ?old-args) ?priority]
  [(cascade-confidence ?purity ?is-stale) ?confidence]
  [(cascade-reasoning ?is-stale ?purity) ?reasoning]]

;; Example result:
{:changed-var 'my.app/config
 :dependents
 [{:var 'my.app/process-user
   :cascade-priority :high
   :confidence 0.6  ; NOTE: Lower than before - we don't know if it was CALLED
   :reasoning "Dependent not re-evaled since config changed. 
               CAVEAT: We don't know if process-user was called after the change,
               or if it captured the old config value. Static analysis only."
   :recommendation "Re-eval process-user to be safe"}
  
  {:var 'my.app/validate
   :cascade-priority :medium
   :confidence 0.4
   :reasoning "config change was pure var assignment. validate might be unaffected.
               CAVEAT: If validate was called and closed over old config, 
               it could have stale references we can't detect."
   :recommendation "Check if validate uses changed config keys, or just re-eval to be safe"}
  
  {:var 'my.handlers/api
   :cascade-priority :low
   :confidence 0.3
   :reasoning "api was re-evaled after config change, so DEFINITION is fresh.
               CAVEAT: If api was previously called and returned closures,
               those closures could still reference old config.
               We can't track runtime invocations."
   :recommendation "Definition is fresh, but watch for stale closures in running system"}]
 
 :summary
   "3 dependents found. Priority based on re-eval timing, NOT runtime calls.
    
    ⚠️  IMPORTANT LIMITATIONS:
    - We track var DEFINITIONS, not function INVOCATIONS
    - Cannot detect if functions were called before/after change
    - Cannot track values captured in closures or objects
    - Staleness analysis is POTENTIAL, not definitive
    
    Safest: Re-eval all 3 + reload dependent namespaces
    Pragmatic: Re-eval process-user, test the others, monitor behavior
    Risky: Trust that re-evaled definitions are enough (ignores runtime state)
    
    When in doubt: Restart the application for guaranteed fresh state.
                   REPL restart refreshes definitions but not running 
                   app state (web servers, database connections, caches,
                   long-lived objects with captured closures, etc.)"}
```

**UI Pattern: Post-Eval Cascade Advisor**
```
✅ Evaluated: (def config {:timeout 60})

⚠️  Impact Analysis (Static Dependencies Only):
    5 direct dependents, 23 transitive dependents
    
    NOTE: We track definitions, not runtime calls.
          Can't know if functions were actually invoked.

🔴 HIGH PRIORITY (definition not refreshed):
    • my.app/process-user - Not re-evaled since config changed
    • my.app/validate     - Not re-evaled since config changed
    
    ⚠️  CAVEAT: Even if never called, re-eval ensures definition is fresh
    
    Recommendation: Re-eval these 2 functions

🟡 MEDIUM PRIORITY (definition might be stale):
    • my.handlers/create  - Evaled BEFORE config change
    • my.utils/format     - Transitively depends on process-user
    
    ⚠️  CAVEAT: If called before config change, may have captured old value
    
    Recommendation: Re-eval or run tests to verify behavior

🟢 LOW PRIORITY (definition refreshed):
    • my.api/handler      - Re-evaled AFTER config change
    
    ⚠️  CAVEAT: Definition is fresh, but if previously called and 
                returned closures/objects, those may still reference 
                old config. We can't track runtime state.
    
    Recommendation: Definition OK, but monitor for stale runtime references

💡 Quick fixes:
   • Re-eval all 23 dependents (safest for definitions, most work)
   • Reload entire namespace (re-establishes file order)
   • Restart REPL (guaranteed fresh definitions, loses session state)
   • **Restart application** (guaranteed fresh runtime state, clears all objects/closures)
   
⚠️  Remember: This analysis is based on static dependencies and 
             eval timestamps. We don't know what was CALLED, only 
             what was DEFINED. When critical, restart the application
             for guaranteed fresh state - REPL restart only refreshes
             definitions, not running application state (servers, 
             connections, cached objects, etc.).
```

#### 3. **Side-Effect Detection**
```clojure
;; Find all vars affected by evaluating a statement
;; (dependency-aware side-effect propagation)
'[:find ?dependent-var ?impact-type ?path-length
  :in $ ?stmt-id
  :where
  [?stmt :statement/id ?stmt-id]
  [?stmt :statement/defines-var ?var]
  [?var :var/fqn ?fqn]
  
  ;; Direct assignment side effect
  [(ground :direct-mutation) ?direct-impact]
  
  ;; Find all vars that depend on this var (transitively)
  (or-join [?fqn ?dependent-fqn ?impact-type ?path-length]
    ;; Zero dependents = truly pure
    (and (not [?usage :usage/callee ?var])
         [(ground :none) ?impact-type]
         [(ground 0) ?path-length])
    
    ;; Has dependents = side effect propagation
    (and [?usage :usage/callee ?var]
         [?usage :usage/caller ?dependent-var]
         [?dependent-var :var/fqn ?dependent-fqn]
         [(ground :dependent-mutation) ?impact-type]
         [(ground 1) ?path-length])
    
    ;; Transitive dependents
    (and (transitive-deps ?fqn ?dependent-fqn ?depth)
         [(ground :transitive-mutation) ?impact-type]
         [(= ?path-length ?depth)]))]

;; Result:
{:statement "(def config 42)"
 :purity :impure-var-with-deps
 :direct-dependents 5
 :transitive-dependents 23
 :impact-summary
   "This assignment affects 23 vars total:
    - 5 direct users of config
    - 18 functions that transitively depend on config
    
    All 23 vars are now stale and may need re-evaluation."
 :ui-warning
   "⚠️  Redefining config will affect 23 functions
    💡 Consider: Is this intended? Should you reload dependents?"}
```

**Critical Query: Zero-Dependency Detection**
```clojure
;; Find truly pure var assignments (zero dependents)
'[:find ?stmt ?var
  :where
  [?stmt :statement/defines-var ?var]
  [?var :var/fqn ?fqn]
  
  ;; No usages = pure assignment
  (not [?usage :usage/callee ?var])]

;; These are safe to eval/reorder without side effects
```

#### 4. **Missing Forward Declarations**
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

#### 5. **Reorder Safety Analysis**
```clojure
;; Which statements can be safely reordered?
;; (Dependency-aware purity check)
'[:find ?stmt ?reorder-safe? ?blocker-reason
  :where
  [?stmt :statement/type ?type]
  [?stmt :statement/purity ?purity]
  [?stmt :statement/defines-var ?var]
  
  ;; Check for dependents
  (or-join [?var ?has-deps]
    (and [?usage :usage/callee ?var]
         [(ground true) ?has-deps])
    (and (not [?usage :usage/callee ?var])
         [(ground false) ?has-deps]))
  
  ;; Safe if:
  ;; 1. Pure-isolated (no var assignment)
  ;; 2. Pure with zero dependents
  ;; 3. Pure-with-forward-declare AND has forward declares
  (or-join [?purity ?has-deps ?reorder-safe? ?blocker-reason]
    ;; Case 1: Pure isolated
    (and [(= ?purity :pure-isolated) true]
         [(ground true) ?reorder-safe?]
         [(ground nil) ?blocker-reason])
    
    ;; Case 2: Var definition with zero dependents
    (and [(= ?purity :pure) true]
         [(false? ?has-deps) true]
         [(ground true) ?reorder-safe?]
         [(ground nil) ?blocker-reason])
    
    ;; Case 3: Would be pure with forward declares
    (and [(= ?purity :pure-with-forward-declare) true]
         [?stmt :statement/forward-declares-needed ?decls]
         [(all-present? ?decls) true]
         [(ground true) ?reorder-safe?]
         [(ground nil) ?blocker-reason])
    
    ;; Case 4: Has dependents = NOT safe
    (and [(contains? #{:impure-var-with-deps :impure-var-unknown-deps} ?purity) true]
         [(ground false) ?reorder-safe?]
         [(count-dependents ?var) ?dep-count]
         [(str "Affects " ?dep-count " dependent vars") ?blocker-reason])
    
    ;; Case 5: Other impurity
    (and [(= ?purity :impure-known) true]
         [(ground false) ?reorder-safe?]
         [?stmt :statement/side-effects ?effects]
         [(str "Side effects: " ?effects) ?blocker-reason]))]

;; Result with detailed reasoning:
{:statement "(def config (read-config))"
 :reorder-safe? false
 :blocker-reason "Affects 5 dependent vars"
 :recommendation
   "This var has dependents - reordering will cause staleness.
    If you must reorder, re-eval all 5 dependents after:
    - my.app/process-user
    - my.app/validate
    - my.app/handler
    - my.utils/check
    - my.utils/format"}
```

#### 6. **Session Reproducibility Check**
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

**Real-time Warning in Editor** (with persistence tracking):
```
my/app.clj [⚠️ Runtime divergence detected]

Line 10: (def config {:timeout 30})
         🔴 REPL VALUE DIFFERS: {:timeout 9999}
         ⚠️  EPHEMERAL CHANGE - will revert to 30 on restart
         💾 Save current REPL value to file? [Yes] [No] [Ignore]
         
         ⚠️  5 functions depend on config
         🔴 Redefining config will make dependents stale
         💡 After eval, consider reloading: process-user, validate, handler

Line 15: (defn process [x] (helper x))
         ⚠️  Calls 'helper' before it's defined (line 20)
         💡 Add: (declare helper) at line 10
         🔴 REPL evaled this BEFORE helper was defined - may fail on reload

Line 18: (def internal-const 42)
         ✅ Zero dependents - safe to reorder/redefine
         ✅ File and REPL in sync
         
Line 20: (defn helper [x] (* x 2))
         ⚠️  23 functions transitively depend on helper
         🔴 Redefining helper affects 23 vars total
         ✅ File and REPL in sync
         💡 Consider: Is this change isolated to testing?
         
Line 25: (alter-var-root #'config assoc :timeout 5000)
         🔴 Mutates config (5 direct dependents, 23 transitive)
         ⚠️  REPL skipped this statement
         🔴 File config differs from runtime config
```

**REPL Status Bar**:
```
📊 Surveyor: 47 vars tracked | 3 ephemeral changes | 2 diverged from file
    
    Click for details →
```

**Ephemeral Changes Panel**:
```
⚠️  EPHEMERAL CHANGES (will be lost on restart):

1. my.app/config
   File:  {:timeout 30}
   REPL:  {:timeout 9999}
   Age:   2m 15s
   Impact: 5 direct deps, 23 transitive deps
   [Save to File] [Revert to File] [Keep Ephemeral]

2. my.utils/debug-mode?
   File:  false
   REPL:  true
   Age:   15s
   Impact: 0 deps
   [Save to File] [Revert to File] [Keep Ephemeral]

3. my.handlers/experimental-handler
   File:  <not in file>
   REPL:  (defn experimental-handler ...)
   Age:   5m 42s
   Impact: 1 dep (called by router)
   [Add to File] [Delete from REPL]
   
💡 Total: 3 ephemeral changes affecting 24 vars
   Consider: Save useful changes, revert experiments
```

**Pre-Reload Sanity Check**:
```
About to reload my.app namespace...

Checking for issues:
✅ No missing forward declarations
⚠️  3 var assignments with dependents:
   → (def config ...)         [5 direct, 23 transitive dependents]
   → (def helper ...)         [2 direct, 8 transitive dependents]
   → (defn process ...)       [12 direct, 47 transitive dependents]
   
   All dependents will become stale after reload.
   Recommend: Reload dependent namespaces too

⚠️  2 statements have I/O side effects:
   → (def config (read-config))  [re-runs I/O, may get different value]
   → (println "Loading...")      [will print again]

⚠️  1 statement uses defonce:
   → (defonce db (create-db))    [won't re-run, keeps old state]

❌ 2 out-of-order dependencies detected:
   → process uses helper before helper defined

Recommendation: 
1. Fix order issues before reloading
2. After reload, consider refreshing 62 dependent vars across 4 namespaces

Affected namespaces:
- my.app (23 vars stale)
- my.handlers (15 vars stale)
- my.utils (18 vars stale)
- my.api (6 vars stale)
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
   
   ;; Persistence tracking (NEW: ephemeral vs. persistent changes)
   :var/file-backed?   {:db/index true}    ; Defined in a file?
   :var/ephemeral?     {:db/index true}    ; REPL-only, lost on restart?
   :var/file-diverged? {:db/index true}    ; REPL differs from file?
   
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
   :event/source    {:db/index true}  ; :repl-eval, :file-load, :ns-reload
   
   ;; Persistence tracking (NEW: ephemeral vs. persistent)
   :event/persistent?  {:db/index true}  ; Saved to file?
   :event/file-backed  {:db/index true}  ; File path if persistent
   :event/ephemeral?   {:db/index true}  ; REPL-only, reverts on restart
   
   ;; File-load events (NEW)
   :event/file      {:db/index true}
   :event/statements {:db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/many}
   :event/eval-order {:db/cardinality :db.cardinality/many}
   
   ;; Statement-eval events (NEW)
   :event/statement {:db/valueType :db.type/ref}
   :event/position  {:db/index true}
   :event/out-of-order? {:db/index true}
   
   ;; Impact tracking (NEW: cascade decision support)
   :event/affected-vars {:db/valueType :db.type/ref
                         :db/cardinality :db.cardinality/many}
   :event/stale-dependents {:db/index true}       ; count
   :event/cascade-needed? {:db/index true}        ; bool
   :event/cascade-confidence {:db/index true}     ; 0.0-1.0
   
   ;; Causality tracking
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
   :statement/purity {:db/index true}  ; :pure-isolated, :pure, :pure-with-forward-declare, :impure-var-with-deps, :impure-known
   :statement/downstream-impact {:db/index true}  ; count of affected vars (NEW)
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
