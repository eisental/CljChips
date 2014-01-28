(ns cljchips.init
  (require [cemerick.pomegranate]
           [clojure.tools.nrepl.server :refer (start-server
                                               stop-server
                                               default-handler)]
           [lighttable.nrepl.handler :refer (lighttable-ops)])
  (import (org.redstonechips.cljchips CljChips clj)
          (org.redstonechips RedstoneChips)))

;; Add libray folder to classpath.
(cemerick.pomegranate/add-classpath (CljChips/folder))

(require '[cljchips.command :refer (register-rcx)])
(require '[cljchips.core :refer (rc-prefs rc scheduler)])

;; Setup clj preferences.
(.registerCircuitPreference rc-prefs clj "REPLport" 4555)
(.registerCircuitPreference rc-prefs clj "runREPL" true)
(def REPLport (.getPref rc-prefs "clj.REPLport"))
(def runREPL (.getPref rc-prefs "clj.runREPL"))

;; Register the /rcx command multimethod
(register-rcx)

;; Run REPL server
(if runREPL
  (do
    (defonce server
      (start-server
       :port REPLport
       :handler (default-handler #'lighttable-ops)))
    (println "[CljChips] Waiting for REPL connections on port" (:port server)))
  (println "[CljChips] To run a REPL server execute `/rcprefs clj.runREPL true` and restart server."))

;; Run postinit script after the server is fully loaded
(.runTask scheduler rc
          #(require 'cljchips.postinit))
