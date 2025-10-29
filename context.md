# Context for AI Reviewers: clj-surveyor Project

## What We're Building

**clj-surveyor** is a new approach to analyzing Clojure codebases that treats all code elements as **queryable entities in a relationship graph**, rather than analyzing individual files or namespaces in isolation.

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

## What Success Looks Like

### For Developers:
- Reduce time-to-understand-codebase by 50%+
- Catch runtime-vs-file mismatches before they cause bugs
- Navigate complex codebases with confidence through relationship queries
- Answer questions like "can I safely remove this?" with graph-based impact analysis

### For Tools:
- Query response time <100ms for typical relationship queries
- Capture 99%+ of runtime var definitions and usages
- <50MB memory overhead for medium projects
- Integration with existing tool ecosystem rather than replacement

## Documents to Review

1. **`design-document-v2.md`** - Concise, entity-focused design (300 lines)
   - Big ideas and entity model
   - Query patterns and use case mapping  
   - Temporal challenges and solutions
   
2. **`design-document.md`** - Comprehensive version (1,850 lines) 
   - Detailed research on existing tools
   - Complete implementation phases
   - Extensive examples and comparisons

## Review Guidance

### Most Helpful Feedback:
- **Concrete use cases** you'd want to solve with this approach
- **Missing entity types or relationships** we should consider
- **Query patterns** that would provide valuable insights
- **Technical concerns** about the approach
- **Comparison** with tools you know in other ecosystems

### Less Important Right Now:
- Implementation details (we're still in concept phase)
- Specific technology choices (Datascript, web frameworks, etc.)
- UI/UX specifics (focusing on core model first)

## Context About Clojure Development

If you're not deeply familiar with Clojure development patterns:

- **REPL-driven development**: Developers frequently redefine functions in a live running system
- **Dynamic loading**: Namespaces and dependencies can be added/removed at runtime
- **Hot reloading**: Code changes without restarting the application
- **Macro expansion**: Code can generate other code, making static analysis challenging
- **Java interop**: Clojure runs on JVM, can call Java methods, complicates dependency tracking

This dynamic nature is both Clojure's strength and the source of analysis challenges that clj-surveyor aims to address.

---

**Thank you for reviewing!** We're looking for insights that will help us build something truly valuable for the Clojure development community.