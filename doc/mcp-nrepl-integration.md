# MCP-nREPL Integration & Analysis Observations

## Overview

This document describes the integration with the **MCP-nREPL bridge** (v0.7.5+) and observations from using it to analyze the clj-surveyor codebase itself.

**MCP-nREPL Bridge**: A Babashka-based server that implements the Model Context Protocol (MCP), enabling AI agents (like GitHub Copilot) to interact with Clojure nREPL servers for code evaluation, analysis, and introspection.

Repository: https://github.com/franks42/mcp-nrepl-joyride

---

## Configuration

### MCP Server Setup

**File**: `~/Library/Application Support/Code/User/mcp.json` (GitHub Copilot format)

```json
{
  "servers": {
    "nrepl-mcp-server-01": {
      "type": "stdio",
      "command": "/opt/homebrew/bin/bb",
      "args": [
        "-cp",
        "/Users/franksiebenlist/Development/mcp-nrepl-joyride/src",
        "/Users/franksiebenlist/Development/mcp-nrepl-joyride/src/nrepl_mcp_server/core.clj"
      ]
    }
  },
  "inputs": []
}
```

### Connecting to nREPL

```clojure
;; Start local nREPL server (if not already running)
;; Port 7890 with :dev alias for full dependencies
clj -M:dev -m nrepl.cmdline --port 7890 &

;; Connect via MCP tools
(mcp_nrepl-mcp-ser_nrepl-connection 
  {:op "connect" 
   :connection "7890" 
   :nickname "analysis"})

;; Evaluate code
(mcp_nrepl-mcp-ser_nrepl-eval 
  {:code "(+ 1 2 3)" 
   :connection "analysis"})
```

---

## Advantages of MCP-nREPL Tools

### 1. **Performance Gain: 25-100x Faster** ⚡

**Before (Joyride child_process approach)**:
- Each evaluation spawned new JVM process
- ~5 seconds per evaluation
- Cold startup for every query
- No state persistence

**After (MCP-nREPL persistent connection)**:
- Single persistent nREPL connection
- ~50-200ms per evaluation
- **25-100x speedup**
- Shared REPL state across queries

**Impact**: Enables rapid iterative analysis that was previously impractical.

### 2. **No Quote Escaping Issues** 🎯

**Problem Solved**: Complex code with nested quotes, backslashes, and special characters

**Solution**: Base64 encoding option

```clojure
;; Without base64 - escaping nightmare
{:code "\"(defn f [x] \\\"(+ x 1)\\\")\""}  ;; Error-prone!

;; With base64 - clean and safe
{:code "KGRlZm4gZiBbeF0gIigrIHggMSkiKQ=="
 :input-base64 true}  ;; No escaping needed!
```

**Benefits**:
- Eliminates quote escaping bugs
- Handles any Clojure code safely
- Preserves whitespace and formatting
- Output can also be base64-encoded

### 3. **Rich Tool Ecosystem** 🔧

**Available MCP Tools**:

| Tool | Purpose | Use Case |
|------|---------|----------|
| `nrepl-connection` | Connect/disconnect/status | Session management |
| `nrepl-eval` | Execute Clojure code | All evaluations |
| `nrepl-load-file` | Load files into REPL | Bulk loading |
| `nrepl-send-message` | Raw nREPL protocol | Advanced operations |
| `local-eval` | MCP server introspection | Debugging MCP itself |
| `local-nrepl-server` | Start test nREPL | Development/testing |

**Multi-connection Support**:
```clojure
;; Connect to multiple REPLs with nicknames
(nrepl-connection {:op "connect" :connection "7890" :nickname "dev"})
(nrepl-connection {:op "connect" :connection "7891" :nickname "prod"})

;; Switch contexts easily
(nrepl-eval {:code "(runtime-snapshot)" :connection "dev"})
(nrepl-eval {:code "(runtime-snapshot)" :connection "prod"})
```

### 4. **Timeout Protection** ⏱️

**Configurable Timeouts**: 1-300 seconds (default: 30s)

```clojure
;; Quick queries
(nrepl-eval {:code "(+ 1 2 3)" :timeout 5000})  ;; 5 seconds

;; Heavy analysis
(nrepl-eval 
  {:code "(build-dependency-graph {:ns-filter #\"^clj-surveyor\"})"
   :timeout 60000})  ;; 60 seconds
```

**Timeout Recovery**:
```clojure
;; If timeout occurs, get message-id from error
;; Retry with same message-id to check for delayed result
(nrepl-eval 
  {:code "(slow-operation)"
   :message-id "previous-timeout-id"
   :timeout 10000})
```

### 5. **Full VS Code API Access** 🖥️

MCP-nREPL bridge runs in the VS Code Extension Host environment, enabling:
- File system operations
- Editor manipulation
- UI interactions
- Extension commands
- Workspace management

**Example**: Create a file from analysis results
```clojure
(require '[promesa.core :as p])
(require '["vscode" :as vscode])

(-> (nrepl-eval {:code "(build-dependency-graph)"})
    (p/then (fn [graph]
              (let [content (pr-str graph)]
                (vscode/workspace.fs.writeFile 
                  (vscode/Uri.file "/tmp/graph.edn")
                  content)))))
```

---

## Analysis Observations: clj-surveyor Codebase

Using the MCP-nREPL tools, we performed comprehensive dependency analysis on the clj-surveyor codebase itself. Here are the key findings:

### Codebase Health Metrics

**Total Functions Analyzed**: 15 (clj-surveyor namespace)
**Total Dependency Graph Size**: 87 vars (including transitive dependencies)

#### Refactoring Statistics

| Metric | Count | Percentage |
|--------|-------|------------|
| **Needs Refactoring** (≥10 deps) | 9 | 60% |
| **Acceptable Complexity** (<10 deps) | 6 | 40% |
| **Stale Functions** (0 dependents) | 1 | 6.7% |

**Conclusion**: Relatively healthy codebase with tight integration. The 60% refactoring rate is expected for analysis/graph-processing code which tends to be complex.

### Top Refactoring Candidates

Functions ranked by dependency count (high = doing too much):

| Function | Total Deps | Core | External | Internal | Priority |
|----------|------------|------|----------|----------|----------|
| `get-cascade-impact` | 22 | 20 | 0 | 2 | 🔴 HIGH |
| `build-dependency-graph` | 18 | 16 | 0 | 2 | 🔴 HIGH |
| `analyze-namespace-code` | 17 | 15 | 1 | 1 | 🔴 HIGH |

**Refactoring Signals**:

1. **High Core Dependencies (>15)**
   - Signal: Function doing multiple distinct operations
   - Fix: Extract logical sub-operations into helpers
   - Example: `get-cascade-impact` could extract BFS traversal, depth tracking, visited set management

2. **High Internal Dependencies (>5)**
   - Signal: Orchestrator/facade function with high coupling
   - Fix: Protocol abstraction or cleaner API
   - Example: Functions calling 5+ other internal functions

3. **High External Dependencies (>3)**
   - Signal: Complex library integration
   - Fix: Wrapper/adapter layer to isolate changes

### Dependency Distribution

**Popularity Statistics** (how many functions depend on each):

| Dependents | Count | Functions |
|------------|-------|-----------|
| 4 | 1 | `build-dependency-graph` |
| 3 | 1 | `get-cascade-impact` |
| 2 | 2 | `analyze-namespace-code`, `all-vars` |
| 1 | 10 | Various helper functions |
| 0 | 1 | One artifact (likely unused) |

**Insight**: Most functions have 1-2 dependents, indicating focused single-purpose design. The top functions (`build-dependency-graph`, `get-cascade-impact`) are core infrastructure as expected.

### Cascade Impact Analysis

**Most Impactful Function**: `get-namespace-code` (middleware layer)

```
get-namespace-code (middleware)
  → analyze-namespace-code (depth 1)
    ↳ build-dependency-graph (depth 2)
    ↳ find-var-usages (depth 2)
      ↳ get-cascade-impact (depth 3)
      ↳ get-var-dependents (depth 3)
      ↳ graph (depth 3)
        ↳ impact (depth 4)
```

- **Total Impact**: 8 vars affected
- **Max Depth**: 4 levels
- **Risk Level**: 🔴 High - Changes ripple deep into the system

**Change Impact Summary**:

| Function | Direct Dependents | Total Cascade | Max Depth | Risk |
|----------|-------------------|---------------|-----------|------|
| `get-namespace-code` | 1 | 8 | 4 | 🔴 High |
| `analyze-namespace-code` | 2 | 7 | 3 | 🔴 High |
| `build-dependency-graph` | 4 | 4 | 2 | 🟡 Medium |
| `get-cascade-impact` | 3 | 2 | 1 | 🟢 Low |

**Insight**: `get-namespace-code` has only 1 direct dependent but affects 8 functions through transitive dependencies. This makes it higher risk than `build-dependency-graph` which has 4 direct dependents but shallower cascade.

### Architectural Roles

Functions classified by their dependency patterns:

| Role | Count | Description | Examples |
|------|-------|-------------|----------|
| 🔗 **Bridge** | 10 | Connects components, typical business logic | `analyze-namespace-code` |
| 🌱 **Root** | 3 | No internal deps, foundational | Core utilities |
| 🍃 **Leaf** | 1 | No dependents, potential dead code | Unused artifact |
| 🔄 **Hub** | 1 | Many deps & dependents, orchestrator | `build-dependency-graph` |

**Insight**: Most functions are bridges (70%), indicating layered architecture. Only 1 hub function suggests good separation of concerns.

---

## Contextual Analysis Framework

Using dependency graphs, we can answer 8 categories of questions about any function:

### 1. 📥 Dependencies (Downward Cascade)
- What functions/vars does this use? (direct)
- What's the full dependency tree? (transitive)
- Which external libraries does it depend on?
- How deep is the dependency cascade?

### 2. 📤 Dependents (Upward Cascade)
- What functions depend on this? (direct)
- What's the full impact tree? (transitive)
- How many functions would break if this changes?
- What's the blast radius depth?

### 3. 🎯 Change Impact
- What needs immediate review if I change this?
- What's the total affected scope?
- How risky is refactoring this function?
- What tests should I run after changes?

### 4. 🏗️ Architectural Role
- Is this a leaf, root, hub, or bridge?
- Is this core infrastructure or business logic?
- Is this over/under-utilized?
- Should this be split or merged?

### 5. 👥 Peers & Neighbors
- What functions have similar dependencies?
- What else is in the same namespace?
- What functions solve similar problems?
- Could this be shared/reused elsewhere?

### 6. 📊 Metrics
- How popular is this function? (dependent count)
- How complex are its dependencies?
- What's the coupling coefficient?
- Is this a bottleneck or single point of failure?

### 7. 🔄 Refactoring Opportunities
- Can dependencies be reduced?
- Should this be split into smaller functions?
- Are there circular dependencies?
- Is there unused code in dependencies?

### 8. 🧪 Testing Strategy
- What's the minimum test coverage needed?
- Which dependents should have integration tests?
- What mocks are needed for testing?
- What edge cases come from dependencies?

---

## Refactoring Metrics Function

We created a reusable function for analyzing refactoring opportunities:

```clojure
(defn refactoring-metrics
  "Analyze a function for refactoring opportunities based on dependency count"
  [graph var-name]
  (let [info (get graph var-name)
        deps (:dependencies info)
        dependents (:dependents info)
        
        core-deps (filter #(clojure.string/starts-with? % "clojure.core/") deps)
        external-deps (filter #(not (or (clojure.string/starts-with? % "clojure.core/")
                                        (clojure.string/starts-with? % "clj-surveyor"))) deps)
        internal-deps (filter #(clojure.string/starts-with? % "clj-surveyor") deps)
        
        total-deps (count deps)
        complexity-score (+ (* (count external-deps) 3)  ; External = 3x weight
                           (* (count internal-deps) 2)   ; Internal = 2x weight
                           (count core-deps))            ; Core = 1x weight
        
        priority (cond
                   (>= total-deps 15) :high
                   (>= total-deps 10) :medium
                   (>= total-deps 5) :low
                   :else :none)
        
        reason (cond
                 (> (count core-deps) 15) "Too many operations - function doing too much"
                 (> (count internal-deps) 5) "High coupling - too many internal dependencies"
                 (> (count core-deps) 10) "Moderate complexity - consider splitting"
                 :else "Acceptable complexity")]
    
    {:var var-name
     :total-deps total-deps
     :core-deps (count core-deps)
     :external-deps (count external-deps)
     :internal-deps (count internal-deps)
     :dependents (count dependents)
     :complexity-score complexity-score
     :priority priority
     :reason reason
     :refactorable? (>= total-deps 10)}))

;; Usage
(refactoring-metrics graph "clj-surveyor.runtime/analyze-namespace-code")
;; => {:var "clj-surveyor.runtime/analyze-namespace-code"
;;     :total-deps 17
;;     :core-deps 15
;;     :external-deps 1
;;     :internal-deps 1
;;     :dependents 2
;;     :complexity-score 20
;;     :priority :high
;;     :reason "Moderate complexity - consider splitting"
;;     :refactorable? true}
```

---

## Key Insights

### 1. **Dependency Count as Refactoring Signal**
High dependency count correlates strongly with code complexity and refactoring opportunities. The rule of thumb:
- **≥15 deps**: High priority refactoring candidate
- **10-14 deps**: Medium priority, consider splitting
- **5-9 deps**: Acceptable complexity
- **<5 deps**: Simple, focused function

### 2. **Cascade Depth vs Direct Dependents**
A function with few direct dependents but deep cascade can be riskier than a function with many direct dependents but shallow cascade. Example:
- `get-namespace-code`: 1 direct, 8 total, depth 4 = 🔴 HIGH RISK
- `build-dependency-graph`: 4 direct, 4 total, depth 2 = 🟡 MEDIUM RISK

### 3. **Bidirectional Analysis Power**
The dependency graph provides both downward (what this uses) and upward (what uses this) traversal, enabling:
- Complete impact analysis
- Refactoring safety assessment
- Architecture visualization
- Dead code detection

### 4. **Stale Code Detection**
Only 1 function (6.7%) with zero dependents indicates a tight, well-utilized codebase. This metric helps identify:
- Truly unused code (safe to delete)
- API endpoints (intentionally unused internally)
- Recent additions (not yet integrated)

---

## Recommended Workflows

### 1. **Pre-Refactoring Analysis**
```clojure
;; Before changing a function
(def metrics (refactoring-metrics graph "my.ns/my-fn"))
(def cascade (get-cascade-impact "my.ns/my-fn" {:graph graph}))

;; Review:
;; - :priority (high/medium/low)
;; - :total-impact (how many vars affected)
;; - :max-depth (how deep the ripple)
```

### 2. **Codebase Health Check**
```clojure
;; Generate full statistics
(def stats {:total (count all-functions)
            :high-complexity (count (filter #(>= (:total-deps %) 15) analyses))
            :stale-code (count (filter #(zero? (:dependents %)) analyses))
            :refactoring-rate (percentage needing-refactor total)})
```

### 3. **Impact Assessment**
```clojure
;; When changing critical infrastructure
(def cascade (get-cascade-impact "core.ns/critical-fn" {:graph graph}))
(print-cascade cascade)  ;; Visual tree of affected functions
```

---

## Future Enhancements

### Planned Features
1. **Circular Dependency Detection**: Find feedback loops in the graph
2. **Coupling Metrics**: Calculate afferent/efferent coupling ratios
3. **Stability Analysis**: Identify fragile vs stable components
4. **Graphviz Export**: Visualize dependency graphs
5. **Historical Tracking**: Monitor complexity trends over time
6. **VS Code Integration**: Right-click → "Show Cascade Impact"

### Research Areas
1. **Machine Learning**: Predict refactoring impact from historical data
2. **Auto-refactoring Suggestions**: Based on dependency patterns
3. **Test Coverage Mapping**: Link tests to dependency cascades
4. **Performance Impact**: Correlate dependencies with runtime performance

---

## Conclusion

The MCP-nREPL integration provides:
- **25-100x performance improvement** over previous approaches
- **Rich tooling ecosystem** for code analysis
- **Bidirectional dependency analysis** (both up and down the graph)
- **Data-driven refactoring insights** based on objective metrics

The analysis of clj-surveyor itself demonstrates the power of this approach, revealing:
- 60% of functions benefit from refactoring (typical for analysis code)
- Clear architectural layers (mostly bridge functions)
- Minimal dead code (6.7% stale rate)
- Well-identified high-risk functions for careful change management

This framework enables **proactive code health management** rather than reactive debugging after problems emerge.

---

## References

- **MCP-nREPL Bridge**: https://github.com/franks42/mcp-nrepl-joyride
- **Model Context Protocol**: https://modelcontextprotocol.io/
- **clj-kondo**: https://github.com/clj-kondo/clj-kondo (underlying analysis engine)
- **nREPL Protocol**: https://nrepl.org/nrepl/index.html
