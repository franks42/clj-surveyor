# clj-surveyor
Collection of tools to help survey, introspect, examine, correlate, understand Clojure code and runtime.

## Quick Links

- **[MCP-nREPL Integration & Analysis](doc/mcp-nrepl-integration.md)** - Performance gains, tooling advantages, and codebase analysis observations
- **[Context for AI Assistants](context.md)** - Project overview and development guide
- **[Phase 0 Status](PHASE0-STATUS.md)** - Current implementation status

## Overview

**clj-surveyor** analyzes Clojure codebases using dependency graphs to provide insights about:
- Function dependencies (what uses what)
- Change impact analysis (cascade effects)
- Refactoring opportunities (complexity metrics)
- Stale code detection (unused functions)
- Architectural roles (hub/bridge/leaf classification)

### Key Features

- **Bidirectional Dependency Analysis**: Traverse both dependencies (downward) and dependents (upward)
- **Cascade Impact Prediction**: See how changes ripple through your codebase
- **Refactoring Metrics**: Data-driven insights on code complexity
- **Runtime Integration**: Works with live REPL state, not just files
- **MCP-nREPL Tools**: 25-100x faster analysis via persistent nREPL connections

## Quick Start

```bash
# Start nREPL with clj-surveyor
clj -M:dev -m nrepl.cmdline --port 7890

# In REPL
(require '[clj-surveyor.runtime :as csr])

# Build dependency graph for your project
(def graph (csr/build-dependency-graph {:ns-filter #"^my-project"}))

# Analyze a function
(csr/get-cascade-impact "my.ns/my-fn" {:graph graph})
(csr/refactoring-metrics graph "my.ns/my-fn")

# Find stale code
(csr/find-stale-functions graph #"^my-project")
```

## Documentation

- **[MCP-nREPL Integration](doc/mcp-nrepl-integration.md)** - Tooling setup and analysis findings
- **[Design Document](doc/design-document-v3.md)** - Architecture and philosophy
- **[Implementation Guide](doc/implementation-guide.md)** - Datascript schema and recipes
- **[Phase 0 Test Results](doc/phase0-test-results.md)** - Testing documentation
- **[clj-kondo API Notes](doc/clj-kondo-api-notes.md)** - Integration details
