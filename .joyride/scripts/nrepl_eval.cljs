;; Convenience function to eval code in the clj-surveyor nREPL server
;; Usage: (nrepl-eval "(+ 1 2 3)")

(ns nrepl-eval
  (:require ["child_process" :as cp]
            [clojure.string :as str]))

(def ^:private nrepl-port 7889)

(defn nrepl-eval
  "Eval code in the clj-surveyor nREPL server on port 7889.
   Returns a promise that resolves with the eval result.
   
   Options:
   - code: String of Clojure code to eval
   - print-output?: Boolean, whether to print output (default true)
   
   Examples:
   (nrepl-eval \"(+ 1 2 3)\")
   (nrepl-eval \"(clj-surveyor.runtime/runtime-snapshot)\")
   (nrepl-eval \"(ns-publics 'clojure.core)\" {:print-output? false})"
  ([code] (nrepl-eval code {}))
  ([code {:keys [print-output?] :or {print-output? true}}]
   (js/Promise.
     (fn [resolve reject]
       (let [escaped-code (-> code
                              (str/replace #"\\" "\\\\")
                              (str/replace #"\"" "\\\"")
                              (str/replace #"\n" "\\n"))
             cmd (str "clj -Sdeps '{:deps {nrepl/nrepl {:mvn/version \"1.3.0\"}}}' -M -e "
                      "\"(require '[nrepl.core :as nrepl]) "
                      "(with-open [conn (nrepl/connect :port " nrepl-port ")] "
                      "(let [client (nrepl/client conn 5000)] "
                      "(doseq [msg (nrepl/message client {:op \\\"eval\\\" :code \\\"" escaped-code "\\\"})] "
                      "(when (:value msg) (println \\\"VALUE:\\\" (:value msg))) "
                      "(when (:out msg) (print (:out msg))) "
                      "(when (:err msg) (binding [*out* *err*] (print (:err msg)))))))\""
                      )]
         
         (cp/exec cmd
                  (fn [error stdout stderr]
                    (when print-output?
                      (when (seq stdout) (js/console.log stdout))
                      (when (seq stderr) (js/console.error stderr)))
                    
                    (if error
                      (reject error)
                      (resolve {:stdout stdout :stderr stderr})))))))))

(defn nrepl-eval-sync
  "Synchronous version - awaits the result.
   Use with await in async context."
  [code]
  (nrepl-eval code))

;; Export for use in other scripts
#js {:eval nrepl-eval
     :evalSync nrepl-eval-sync}
