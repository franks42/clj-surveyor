(ns test.demo-file
  "Test namespace for dependency detection")

(defn helper-fn
  "A base function to be called by others"
  [x]
  (* x 10))

(defn caller-1
  "Calls helper-fn"
  [x]
  (+ (helper-fn x) 5))

(defn caller-2
  "Also calls helper-fn twice"
  [x]
  (+ (helper-fn x) (helper-fn (* x 2))))

(defn independent
  "Does not call helper-fn"
  [x]
  (+ x 100))
