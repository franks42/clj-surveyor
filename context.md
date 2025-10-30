# Context for AI Assistants: clj-surveyor Project

## Current Status: Phase 0 Implementation (Active Development)

**clj-surveyor** is a new approach to analyzing Clojure codebases that treats all code elements as **queryable entities in a relationship graph**, rather than analyzing individual files or namespaces in isolation.

### Recent Progress (October 2025)
- ✅ Phase 0 core functionality implemented and tested
- ✅ nREPL middleware for capturing eval'd code
- ✅ clj-kondo integration for high-confidence dependency analysis
- ✅ In-memory analysis (no temp files)
- ✅ Full runtime dependency graph builder
- 🚧 Moving toward Phase 1 (Datascript entity graph)

## What We're Building

## The Core Problem We're Solving

### The Clojure Development Paradox
- **Language Philosophy**: Clojure promotes immutability and managed state changes
- **Development Reality**: The development process itself is highly mutable - vars get redefined in the REPL, namespaces are reloaded, dependencies change dynamically
- **Current Tools Gap**: Existing analysis tools focus on static file analysis, missing the runtime mutations that cause bugs and surprises

### The "Always Out of Sync" Challenge  
- Runtime state changes faster than we can observe and record
- By the time we capture and analyze entities, the runtime has moved on
- Traditional tools pretend this isn't a problem; we embrace and make it visible

## Our Key Insights

### 1. **FQN Usage vs Requires**
- `require` statements are just convenience for developers, not true dependencies
- Real dependencies are **fully qualified name (FQN) usage** in the actual code
- If all vars were written as FQNs, no requires would be needed

### 2. **Entity-Relationship Model**
- Every code element (var, namespace, usage, dependency) becomes a first-class entity
- Entities have attributes, relationships, and temporal metadata
- Insights emerge from querying the relationship graph, not individual analysis

### 3. **Runtime-First Analysis**
- REPL-driven development makes runtime the source of truth, not files  
- Static analysis shows intention; runtime analysis shows reality
- Gap between file state and runtime state is where bugs hide

## What We Want Feedback On

### Primary Questions:
1. **Use Cases**: What specific analysis scenarios would be most valuable?
2. **Entity Model**: Are we missing important entity types or relationships?
3. **Query Patterns**: What kinds of insights should the relationship graph enable?
4. **Temporal Handling**: How should we handle the staleness/confidence challenge?

### Secondary Questions:
5. **Differentiation**: How does this compare to your knowledge of existing tools?
6. **Implementation**: What technical challenges do you foresee?
7. **Adoption**: What would make developers want to use this?

## Current Design Approach

### Entity Types:
- **Var Entities**: Functions, variables, macros with temporal metadata
- **Namespace Entities**: Container entities with convenience relationships  
- **Usage Entities**: The key innovation - who calls what, where, when
- **Dependency Entities**: True dependencies vs convenience requires
- **Change Event Entities**: Historical mutations with causality tracking
- **Staleness Entities**: Track sync state and confidence levels

### Query-Driven Philosophy:
Instead of "analyze this function," we ask "show me everything that would break if I change this function" via graph traversal queries.

### Temporal Awareness:
Every entity has staleness metadata, every query includes confidence levels, UI shows data freshness with visual indicators.

---

## FOR AI REVIEWERS AND ASSISTANTS

### Most Helpful Contributions:
- **Concrete use cases** to solve with this approach
- **Missing entity types or relationships** to consider
- **Query patterns** that would provide valuable insights
- **Technical concerns** about the approach
- **Comparison** with tools in other ecosystems

### When Working on This Project:
1. **Read `doc/phase0-test-results.md` first** - See what's working
2. **Test changes locally** before committing (use test/test/demo_file.clj)
3. **Update this context.md** when making significant changes
4. **Commit frequently** with descriptive messages
5. **Run dependency analysis** to verify changes don't break things

### Communication Style:
- Be direct and concise
- Show code examples when helpful
- Test before claiming something works
- Document testing results
- No unnecessary markdown summaries unless requested

---

**Thank you for contributing!** This project aims to build something truly valuable for the Clojure development community.

This dynamic nature is both Clojure's strength and the source of analysis challenges that clj-surveyor aims to address.

---

## PHASE 0 IMPLEMENTATION STATUS (Current Work)

### What's Working Now

#### 1. nREPL Middleware (`src/clj_surveyor/middleware.clj`)
**Purpose**: Capture eval'd code that has no source files

**Key Components**:
- `namespace-code-store` - Atom storing code by namespace
- `wrap-surveyor` - Middleware intercepting eval operations
- `get-namespace-code` - Public API to retrieve captured code

**How It Works**:
```clojure
;; Middleware intercepts every nREPL eval
(when (and (= op "eval") code ns)
  (swap! namespace-code-store update (symbol ns) (fnil conj []) code))
```

**Starting nREPL with Middleware**:
```clojure
;; In dev/clj_surveyor_dev.clj
(require '[nrepl.server :as nrepl])
(require '[clj-surveyor.middleware :as surveyor-mw])
(def server (nrepl/start-server 
              :port 7889
              :handler (nrepl/default-handler #'surveyor-mw/wrap-surveyor)))
```

#### 2. Runtime Analysis (`src/clj_surveyor/runtime.clj`)
**Purpose**: Analyze dependencies using clj-kondo on runtime code

**Key Functions**:

**`analyze-namespace-code [ns-obj]`**
- Uses clj-kondo to analyze namespace code
- Tries middleware-captured code first, falls back to file
- Returns `{:var-usages [...] :var-definitions [...]}`
- Uses in-memory approach: `(with-in-str code (kondo/run! {:lint ["-"] ...}))`

**`get-var-dependents [var-fqn]`**
- Find all vars that call the target var
- Returns: `{:target-var ... :dependents [...] :count N :confidence :high}`
- Example: `(get-var-dependents "test.demo-file/helper-fn")`
  - Returns caller-1 and caller-2 with row/col positions

**`build-dependency-graph [opts]`** ⭐ NEW
- Analyze ALL loaded namespaces
- Build complete bidirectional dependency graph
- Returns: `{var-fqn {:dependents #{...} :dependencies #{...}}}`
- Optional `:ns-filter` regex to limit scope
- Example:
  ```clojure
  (build-dependency-graph {:ns-filter #"^my-project"})
  ;; => {"my.ns/foo" {:dependents #{"my.ns/bar"}
  ;;                  :dependencies #{"clojure.core/map"}}
  ;;     ...}
  ```

**Other Utilities**:
- `all-vars` - Get all vars, optionally filtered by namespace pattern
- `var-info` - Extract metadata about a var
- `namespace-summary` - Stats about a namespace
- `runtime-snapshot` - Capture current runtime state

#### 3. clj-kondo Integration
**Why clj-kondo?**
- High-confidence static analysis
- Complete var usage information
- Row/col positions for navigation
- Handles macros and complex code

**API Pattern** (documented in `doc/clj-kondo-api-notes.md`):
```clojure
(with-in-str code-string
  (kondo/run! {:lint ["-"]  ;; stdin, not :stdin parameter
               :config {:output {:analysis {:var-usages true
                                           :var-definitions true}}}}))
```

**Returns**:
```clojure
{:analysis 
  {:var-usages [{:name 'helper-fn        ;; var being called
                 :from 'test.demo-file   ;; namespace of caller
                 :from-var 'caller-1     ;; function making call
                 :to 'test.demo-file     ;; namespace of callee
                 :row 12 :col 6          ;; location
                 :arity 1}]
   :var-definitions [...]}}
```

### Testing Results

**Test File**: `test/test/demo_file.clj`
```clojure
(defn helper-fn [x] (* x 10))
(defn caller-1 [x] (+ (helper-fn x) 5))
(defn caller-2 [x] (+ (helper-fn x) (helper-fn (* x 2))))
(defn independent [x] (+ x 100))
```

**Verified Working**:
```clojure
;; Single var lookup
(get-var-dependents "test.demo-file/helper-fn")
;; => {:dependents ({:user-fqn test.demo-file/caller-1 ...}
;;                  {:user-fqn test.demo-file/caller-2 ...}
;;                  {:user-fqn test.demo-file/caller-2 ...})
;;     :count 3  ;; caller-2 calls it twice
;;     :confidence :high
;;     :method :clj-kondo-analysis}

;; Full graph
(build-dependency-graph {:ns-filter #"^test\.demo"})
;; => {"test.demo-file/helper-fn" {:dependents #{"test.demo-file/caller-1" 
;;                                               "test.demo-file/caller-2"}
;;                                 :dependencies #{"clojure.core/*"}}
;;     "test.demo-file/caller-1" {:dependencies #{"test.demo-file/helper-fn"
;;                                                "clojure.core/+"}}
;;     ...}
```

See `doc/phase0-test-results.md` for comprehensive testing documentation.

### Working with the Runtime

#### Local REPL Testing
```bash
# Start REPL with :dev alias
clj -M:dev

# Load and test
(require 'clj-surveyor.runtime)
(load-file "test/test/demo_file.clj")
(clj-surveyor.runtime/get-var-dependents "test.demo-file/helper-fn")
```

#### nREPL Server Setup
```bash
# Start nREPL with middleware (port 7889)
clj -M:dev -e "(load-file \"dev/clj_surveyor_dev.clj\") (dev.clj-surveyor-dev/start-dev)"

# Check server running
lsof -i :7889
```

#### Connecting to nREPL
**From Joyride** (VS Code):
```clojure
;; See .joyride/scripts/nrepl_eval.cljs
(nrepl-eval "(clj-surveyor.runtime/get-var-dependents \"my.ns/my-fn\")")
```

**From Terminal**:
```bash
clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version "1.3.0"}}}' -M -e \
  "(require '[nrepl.core :as nrepl])
   (with-open [conn (nrepl/connect :port 7889)]
     (let [client (nrepl/client conn 5000)]
       (doseq [msg (nrepl/message client {:op \"eval\" 
                                          :code \"(+ 1 2 3)\"})]
         (when (:value msg) (println \"VALUE:\" (:value msg))))))"
```

### Key Files to Know

**Implementation**:
- `src/clj_surveyor/middleware.clj` - nREPL middleware for code capture
- `src/clj_surveyor/runtime.clj` - Analysis functions (Phase 0)
- `deps.edn` - Dependencies (includes clj-kondo, nrepl, clj-ns-browser)

**Testing**:
- `test/test/demo_file.clj` - Test namespace with known dependencies
- `doc/phase0-test-results.md` - Comprehensive test documentation

**Documentation**:
- `doc/design-document-v3.md` - Current design (File/Statement entities)
- `doc/implementation-guide.md` - Datascript schema and code recipes
- `doc/clj-kondo-api-notes.md` - clj-kondo API research and usage
- `doc/phase0-status.md` - Phase 0 completion checklist

**Development**:
- `dev/clj_surveyor_dev.clj` - Dev helpers for starting nREPL
- `.joyride/scripts/nrepl_eval.cljs` - VS Code Joyride helper for nREPL

### Common Development Tasks

**Add a new analysis function**:
1. Add to `src/clj_surveyor/runtime.clj`
2. Use `analyze-namespace-code` to get clj-kondo data
3. Process `:var-usages` or `:var-definitions` as needed
4. Test locally with `clj -M:dev`
5. Test in nREPL via middleware

**Debug middleware code capture**:
```clojure
;; Check what's been captured
(require 'clj-surveyor.middleware :as mw)
(mw/get-all-namespace-codes)  ;; See all captured code
(mw/get-namespace-code 'my.ns)  ;; Get specific namespace
```

**Debug clj-kondo analysis**:
```clojure
;; Test analysis directly
(require 'clj-surveyor.runtime :as rt)
(rt/analyze-namespace-code (find-ns 'test.demo-file))
;; Check :var-usages and :var-definitions
```

### Next Steps (Phase 1)

- [ ] Implement Datascript entity graph (schema in implementation-guide.md)
- [ ] Add event tracking in middleware (eval events → entities)
- [ ] Create query API over Datascript
- [ ] Add temporal/staleness tracking
- [ ] Implement cascade analysis
- [ ] Create proper test suite

### Git Workflow

**Recent Tags**:
- `v0.4.0-phase0-dev` - Phase 0 development milestone

**Branches**:
- `main` - Active development (Phase 0 complete, moving to Phase 1)

**Committing Changes**:
```bash
git add -A
git commit -m "Description"
git push
```

**Creating Tags**:
```bash
git tag -a v0.x.x -m "Description"
git push origin v0.x.x
```

---

## DESIGN PHILOSOPHY

### The Core Problem We're Solving