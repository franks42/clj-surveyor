(ns nrepl-client
  (:require ["net" :as net]
            [promesa.core :as p]))

;; nREPL connection state
(defonce state (atom {:socket nil
                      :responses {}
                      :msg-id 0}))

;; Simple bencode encoder
(defn bencode-string [s]
  (str (count s) ":" s))

(defn bencode-dict [m]
  (str "d"
       (apply str
              (mapcat (fn [[k v]]
                        [(bencode-string (name k))
                         (bencode-string (str v))])
                      m))
       "e"))

;; Connect to nREPL
(defn connect! 
  ([port] (connect! "localhost" port))
  ([host port]
   (let [socket (net/createConnection #js {:port port :host host})
         response-buffer (atom "")]
     
     (.setEncoding socket "utf8")
     
     ;; Handle incoming data
     (.on socket "data" 
          (fn [data]
            (swap! response-buffer str data)
            ;; Parse bencode responses (simplified - just store raw for now)
            (js/console.log "nREPL response:" data)))
     
     (.on socket "error" 
          (fn [err]
            (js/console.error "nREPL error:" err)))
     
     (swap! state assoc :socket socket)
     socket)))

;; Eval code in nREPL
(defn eval-code [code]
  (let [socket (:socket @state)
        id (str "msg-" (swap! state update :msg-id inc))
        msg (bencode-dict {:op "eval"
                           :code code
                           :id id})]
    
    (when socket
      (.write socket msg)
      
      ;; Return promise that resolves with response
      (p/create
        (fn [resolve _reject]
          (let [timeout (js/setTimeout
                          (fn [] (resolve {:error "timeout"}))
                          5000)
                handler (fn [data]
                          (js/clearTimeout timeout)
                          (resolve {:id id :data (str data)}))]
            (.once socket "data" handler))))))))

;; Convenience function to eval and get result
(defn eval-sync [code]
  (eval-code code))

;; Export public API
#js {:connect connect!
     :eval eval-code
     :evalSync eval-sync}