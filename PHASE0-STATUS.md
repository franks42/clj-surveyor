# Phase 0 Status

## Current State (2024-10-29)

### What's Working ✅

- **nREPL Server**: Running on port 7889 (background process, PID 98755)
- **Middleware**: `clj-surveyor.middleware/wrap-surveyor` active and capturing eval'd code
- **Runtime Introspection**: Basic functions implemented:
  - `all-vars` - Get all vars, optionally filtered by namespace
  - `var-info` - Extract metadata from vars
  - `namespace-summary` - Get var counts and breakdowns
  - `runtime-snapshot` - Capture runtime state (110 namespaces, 3250 vars)
- **clj-ns-browser**: UI running, accessible via `(sdoc)`
- **clj-info**: Enhanced documentation available via `(info symbol)`
- **Convenience Helper**: `.joyride/scripts/nrepl_eval.cljs` for eval without prompts

### In Development ⚙️

- **Dependency Discovery** (`get-var-dependents`):
  - Middleware now captures eval'd code in `namespace-code-store`
  - `analyze-namespace-code` function created to use clj-kondo
  - `find-var-usages` updated to parse clj-kondo analysis results
  - **Status**: Code written but clj-kondo API usage needs verification
  - **Next**: Test with captured code and verify `:var-usages` extraction

### Architecture

```
┌─────────────────────────────────────────────┐
│ nREPL Client (Joyride/terminal)             │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│ nREPL Server (port 7889)                    │
│  ├─ wrap-surveyor middleware                │
│  │   └─ Captures eval'd code → storage      │
│  └─ Standard nREPL handler                  │
└─────────────────┬───────────────────────────┘
                  │
                  ▼
┌─────────────────────────────────────────────┐
│ Runtime Introspection (clj-surveyor.runtime)│
│  ├─ analyze-namespace-code                  │
│  │   ├─ Get code from middleware store      │
│  │   ├─ Fallback to file if available       │
│  │   └─ Run clj-kondo analysis               │
│  └─ find-var-usages                         │
│      └─ Parse :var-usages from analysis     │
└─────────────────────────────────────────────┘
```

### Key Design Decisions

1. **Capture Source at Eval Time**: Middleware stores code strings to enable analysis of REPL-defined vars
2. **clj-kondo for Static Analysis**: High-confidence dependency tracking via actual AST analysis
3. **Graceful Fallbacks**: Try captured code first, then file-based source
4. **Per-Namespace Storage**: Code organized by namespace for efficient lookup

## Next Steps

### Immediate (Testing & Validation)

1. **Verify clj-kondo API**:
   ```clojure
   ;; Test locally to confirm API usage
   (require 'clj-kondo.core)
   (clj-kondo.core/run! {:lint ["-"]
                         :lang :clj
                         :cache false
                         :config {:output {:analysis {:var-usages true}}}
                         :stdin "(ns test) (defn f [x] (* x 10)) (defn g [x] (f x))"})
   ```

2. **Test with Real Data**:
   - Restart nREPL with updated middleware
   - Eval test code: `(ns test.live) (defn helper [x] (* x 10)) (defn user [x] (helper x))`
   - Query: `(get-var-dependents "test.live/helper")`
   - Expect: `{:dependents [{:user-fqn "test.live/user" :ref-type :call ...}] :count 1}`

3. **File-based Test**:
   - Load `test.demo-file` namespace
   - Query: `(get-var-dependents "test.demo-file/helper-fn")`
   - Expect: Should find `caller-1` and `caller-2` as dependents

### Short-term (Phase 0 Completion)

- **Datascript Integration**: Initialize entity graph with schema from implementation guide
- **Event Tracking**: Enhance middleware to create :event entities for each eval
- **Staleness Detection**: Compare file vs runtime state
- **Test Suite**: Create `test/clj_surveyor/runtime_test.clj`

### Medium-term (Phase 1)

- **Custom nREPL Ops**: Add `surveyor-dependents`, `surveyor-staleness`, `surveyor-cascade`
- **File Entity Tracking**: Track which statements came from files vs REPL
- **Cascade Analysis**: Implement dependency-aware cascade preview
- **CLI Interface**: Command-line tool for querying surveyor data

## Known Limitations

- **REPL-only code**: Middleware must be active to capture source (no retroactive analysis)
- **Single namespace**: Current `find-var-usages` only searches within target var's namespace
- **No cross-namespace deps**: Phase 0 doesn't track requires/imports yet
- **Static analysis only**: Doesn't catch dynamic `resolve`, `eval`, etc.

## Files Changed in This Phase

- `deps.edn`: Added clj-kondo dependency
- `src/clj_surveyor/middleware.clj`: Code capture functionality
- `src/clj_surveyor/runtime.clj`: clj-kondo integration
- `.joyride/scripts/nrepl_eval.cljs`: Convenience eval helper
- `test/test/demo_file.clj`: Test namespace with known dependencies

## Running the System

```bash
# Start nREPL server (if not running)
nohup clj -M:dev -e "(require '[nrepl.server :as nrepl] '[clj-surveyor.middleware :as mw]) (nrepl/start-server :port 7889 :handler (nrepl/default-handler #'mw/wrap-surveyor))" > /tmp/clj-surveyor-nrepl.log 2>&1 &

# Connect and test
clj -M -e "(require '[nrepl.core :as nrepl]) (with-open [conn (nrepl/connect :port 7889)] (let [client (nrepl/client conn 5000)] (doseq [msg (nrepl/message client {:op \"eval\" :code \"(require 'clj-surveyor.runtime) (clj-surveyor.runtime/runtime-snapshot)\"})] (when (:value msg) (println (:value msg))))))"
```

## Resources

- **Design**: `doc/design-document-v3.md`
- **Implementation Guide**: `doc/implementation-guide.md`
- **clj-kondo Docs**: https://github.com/clj-kondo/clj-kondo/blob/master/doc/hooks.md
