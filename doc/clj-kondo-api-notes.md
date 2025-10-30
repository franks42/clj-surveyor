# clj-kondo API Research Notes

## API Signature

```clojure
(clj-kondo.core/run! {:lint [paths]           
                      :lang :clj              ; optional: :clj, :cljs, :cljc
                      :cache boolean          ; default true
                      :config {:output {:analysis {...}}}})
```

## Analysis Configuration

Enable analysis via `:config` map:

```clojure
{:config {:output {:analysis true}}}  ; Enable all analysis

;; Or selectively:
{:config {:output {:analysis {:var-usages true
                              :var-definitions true
                              :locals true
                              :keywords true
                              :arglists true}}}}
```

## Input Methods

1. **File paths**: `{:lint ["src" "test" "path/to/file.clj"]}`
2. **Stdin**: `{:lint ["-"]}` (reads from `*in*`)
3. **Classpath**: `{:lint ["$(clj -Spath)"]}`

**Important**: There is NO `:stdin` parameter. Code strings must be written to a temp file or piped to stdin.

## Return Structure

```clojure
{:findings [...]       ; Lint warnings/errors
 :summary {...}        ; Error/warning counts
 :analysis {           ; When :analysis configured
   :var-usages [{:filename string
                 :row int
                 :col int
                 :end-row int
                 :end-col int
                 :name-row int
                 :name-col int
                 :name-end-row int
                 :name-end-col int
                 :name symbol        ; The used var name
                 :from namespace     ; The namespace using it
                 :to namespace       ; The namespace where it's defined
                 :from-var symbol    ; Optional: the var doing the using
                 :arity int}]        ; Optional: if it's a call
   
   :var-definitions [{:filename string
                      :row int
                      :col int
                      :end-row int
                      :end-col int
                      :ns namespace
                      :name symbol
                      :defined-by symbol  ; e.g. clojure.core/defn
                      :fixed-arities #{...}
                      :varargs-min-arity int
                      :private boolean
                      :macro boolean
                      :doc string
                      :deprecated boolean/string}]
   
   :namespace-definitions [...]
   :namespace-usages [...]
   :locals [...]
   :local-usages [...]}}
```

## Usage Patterns for Dependency Analysis

### Finding var usages within a namespace:

```clojure
(let [analysis (:analysis (kondo/run! {:lint [file-path]
                                       :config {:output {:analysis {:var-usages true}}}}))
      var-usages (:var-usages analysis)]
  (->> var-usages
       (filter (fn [usage]
                 (and (= (:name usage) 'target-fn)
                      (= (:to usage) 'target.namespace))))
       (map :from-var)))  ; Get the vars that use target-fn
```

### Key Fields:
- `:name` - The symbol being referenced
- `:from` - Namespace where the reference occurs
- `:to` - Namespace where the symbol is defined
- `:from-var` - The specific var (function) making the reference
- `:arity` - If it's a function call, the number of arguments

## For clj-surveyor Implementation

### Approach for REPL-eval'd code:

1. Middleware captures code string in `namespace-code-store`
2. When analyzing dependencies:
   - Get code string for namespace
   - Write to temporary file
   - Call `kondo/run!` with temp file path
   - Parse `:var-usages` from analysis
   - Clean up temp file

```clojure
(let [temp-file (java.io.File/createTempFile "surveyor-" ".clj")
      _ (spit temp-file code-string)
      result (kondo/run! {:lint [(.getAbsolutePath temp-file)]
                          :config {:output {:analysis {:var-usages true}}}})
      _ (.delete temp-file)]
  (:analysis result))
```

### Fallback for file-based code:

```clojure
(kondo/run! {:lint [file-path]
             :config {:output {:analysis {:var-usages true}}}})
```

## References

- [clj-kondo API docs](https://github.com/clj-kondo/clj-kondo/blob/master/API.md)
- [Analysis README](https://github.com/clj-kondo/clj-kondo/blob/master/analysis/README.md)
- [Config docs](https://github.com/clj-kondo/clj-kondo/blob/master/doc/config.md)
- [Example tools](https://github.com/clj-kondo/clj-kondo/tree/master/analysis/src/clj_kondo/tools)

## Common Pitfalls

1. ❌ Trying to use `:stdin` parameter - doesn't exist
2. ❌ Expecting `:to-ns` in var-usages - it's just `:to`
3. ❌ Forgetting to enable analysis via `:config`
4. ❌ Not handling temp file cleanup
5. ✅ Use temp file approach for code strings
6. ✅ Check `:name` and `:to` fields for matching
7. ✅ Use `:from-var` to identify the calling function
