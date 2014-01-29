(ns cljchips.factory
  (import (org.redstonechips.circuit Circuit)
          (org.redstonechips.cljchips CljChips)
          (java.io File))
  (require [clojure.string :as str]))

(defn find-free-ns
  "Returns the first free namespace name for the supplied name.
   Returns a symbol of `name` or the first free name found by
   adding an id number after the name (`name`1 `name`2 etc.)."
  [name]
  (if-not (find-ns name)
    (symbol name)
    (loop [id 1]
      (let [ns-name (str name id)]
        (if-not (find-ns (symbol ns-name))
          (symbol ns-name)
          (recur (inc id)))))))

(defn setup-circuit-ns
  "Creates the circuit namespace and a default Circuit proxy.
   Creates a circuit var containing the proxy instance in
   the new namespace.

   Returns the circuit namespace."
  [name]
  (let [ns-sym (symbol (find-free-ns name))
        circuit-ns (create-ns ns-sym)
        circuit-var (intern circuit-ns 'circuit)]
    (alter-var-root circuit-var
                    (fn [_] (proxy [Circuit] []
                             (init [args] @circuit-var))))
    circuit-ns))

(defn ns-file-name
  "Returns the filename for a namespace name."
  [sym]
  (str (-> (name sym)
           (str/replace #"-" "_")
           (str/replace #"\." File/separator))
       ".clj"))

(defn load-clj-circuit
  "Creates a circuit namespace and loads the script file for
   the name argument, inside the new namespace.

   Returns the circuit namespace."
  [name]
  (let [circuit-ns (setup-circuit-ns name)]
    (binding [*ns* circuit-ns]
      (require '[cljchips.core :refer :all]
               '[clojure.core :refer :all])
      (try
        (load-file (str CljChips/folder File/separator
                        (ns-file-name name)))
        (catch Exception e
          (remove-ns (symbol (.getName circuit-ns)))
          (throw e))))
    circuit-ns))
