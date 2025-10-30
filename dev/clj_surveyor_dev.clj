;; Dev helper to start clj-ns-browser UI and an nREPL server for interactive development.
;; Usage: clj -Sdeps-file deps.edn -M:dev -m dev.clj-surveyor-dev/start-dev

(ns dev.clj-surveyor-dev
  (:require
    [clojure.tools.logging :as log]
    ;; clj-ns-browser (local) provides UI helpers
    [clj-ns-browser.core :as nsb]
    ;; nREPL server - provided by the environment; if missing, starting REPL may still work
    [nrepl.server :as nrepl]))

(defonce ^:private server-atom (atom nil))

(defn start-browser
  "Start the clj-ns-browser UI in a future (non-blocking). Returns the future." 
  []
  (log/info "Starting clj-ns-browser UI (in background)")
  (future
    ;; call main / helper that constructs the UI
    (try
      (nsb/-main)
      (catch Throwable t
        (log/error t "Failed to start clj-ns-browser UI")))))

(defn start-nrepl
  "Start an nREPL server on the given port (default 7888). Returns server instance.
   If a server is already running (via this helper) it will be returned instead of starting a new one." 
  ([] (start-nrepl 7888))
  ([port]
   (if-let [s @server-atom]
     (do (log/info "nREPL server already running") s)
     (let [s (nrepl/start-server :port port)]
       (reset! server-atom s)
       (log/info "Started nREPL server on port" port)
       s))))

(defn stop-nrepl
  "Stop the nREPL server started with start-nrepl (if any)." []
  (when-let [s @server-atom]
    (try
      (nrepl/stop-server s)
      (reset! server-atom nil)
      (log/info "Stopped nREPL server")
      (catch Throwable t
        (log/error t "Error stopping nREPL server")))))

(defn start-dev
  "Convenience entry point to start clj-ns-browser UI and an nREPL server.
   Intended to be invoked via `clj -Sdeps-file deps.edn -M:dev -m dev.clj-surveyor-dev/start-dev`.
   Returns a map with :ui (future) and :nrepl (server instance).
   Note: if you already have an nREPL running on the same port, pick a different port or stop it first."
  ([] (start-dev 7888))
  ([port]
   (let [ui-f (start-browser)
         repl (try
                (start-nrepl port)
                (catch Throwable t
                  (log/error t "Could not start nREPL server; maybe port in use")
                  nil))]
     {:ui ui-f :nrepl repl}))
