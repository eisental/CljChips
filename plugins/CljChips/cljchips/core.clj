(ns cljchips.core
  (:import (org.redstonechips RedstoneChips)
           (org.redstonechips.chip ChipListenerImpl Chip)
           (org.redstonechips.circuit Circuit)
           (org.redstonechips.wireless Transmitter Receiver)
           (org.redstonechips.cljchips CljChips)
           (org.redstonechips.util BooleanSubset))
  (:require [cemerick.pomegranate :refer (add-dependencies)]))

;; Top-level objects

(def rc (RedstoneChips/inst))
(def plugin (CljChips/inst))
(def server (.getServer rc))
(def main-world #(first (.getWorlds server)))
(def scheduler (.getScheduler server))
(def chipManager (.chipManager rc))
(def all-chips (.getAllChips chipManager))

(defn on-server-thread
  "Invokes f on the main bukkit thread using a synchronized scheduler task.
   Use `:bind-out true` to see output on the calling thread."
  [f & {:keys [bind-out]}]
  (if bind-out
    (let [out *out*]
      (.runTask scheduler plugin #(binding [*out* out] (f))))
    (.runTask scheduler plugin f)))

(defn broadcast-message [msg] (.broadcastMessage server msg))

;; Dependencies

(defn inject-deps
  "Adds dependencies from maven central or clojars.org according to coords vector.
   Uses another thread to download. Invokes callback on server thread when done."
  [coords callback]
  (let [reps (merge cemerick.pomegranate.aether/maven-central
                    {"clojars" "http://clojars.org/repo"})]
    (doto (Thread.
           (fn [] ((add-dependencies :coordinates coords
                                    :repositories reps)
                  (on-server-thread
                   (fn []
                     (broadcast-message (str "CljChips> Done adding dependencies: " coords))
                     (when callback (callback)))))))
      .start)))

;; CommandSender implementation

(defn strip-color [s] (clojure.string/replace s #"§." ""))

(def std-command-sender (reify org.bukkit.command.CommandSender
                          (getName [this]
                            "clojure CommandSender")
                          (getServer [this] (server))
                          (^void sendMessage [this ^String message]
                            (println (strip-color message)))))

;; Basic ChipListener implementation

(def std-chip-listener (proxy [ChipListenerImpl] []
                         (circuitMessage [^Chip chip msg]
                           (println chip msg))
                         (inputChanged [^Chip chip idx val])
                         (outputChanged [^Chip chip idx val])
                         (chipDisabled [^Chip chip])))

;; Time

(def game-time {:day 0 :noon 6000 :evening 12000 :night 18000})

(defn set-time
  ([time] (set-time (main-world) time))
  ([world time] (.setFullTime world time)))

(defn get-time
  ([] (get-time (main-world)))
  ([world] (.getTime world)))

;; Dealing with Chips

(defn chip-by-id [id]
  (.getById all-chips (str id)))

(defn reset-chip
  "Resets a chip. Same as /rcreset"
  [chip]
  (on-server-thread #(.resetChip chipManager chip std-command-sender) :bind-out true))

;; Wireless

(defn make-transmitter [circuit channel len]
  (let [t (Transmitter.)]
    (.init t (.activator circuit) channel len circuit)
    t))

(defn make-receiver [circuit channel len receive-fn]
  (let [rec (proxy [Receiver] []
              (receive [^BooleanSubset bits]
                (receive-fn bits)))]
    (.init rec (.activator circuit) channel len circuit)
    rec))

;; circuit macros

(defmacro $
  "Call a circuit method or access a circuit field.
   Syntax: ($ <fn> <args>) or ($ <field>)"
  [name & body]
  (let [dot-name (symbol (str "." name))]
    `(~dot-name ^Circuit ~'circuit ~@body)))

(defmacro on
  "Define a circuit function. Inside the function `this` refers to the circuit.
   Syntax: (on <fn-name> [<args>] <fn-body>"
  [fn-name args & body]
  `(update-proxy ~'circuit {(str '~fn-name) (fn [~'this ~@args] ~@body)}))
