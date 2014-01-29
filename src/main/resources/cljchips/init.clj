(ns cljchips.init
  (require [cemerick.pomegranate]
           [clojure.tools.nrepl.server :refer (start-server
                                               stop-server
                                               default-handler)]
           [lighttable.nrepl.handler :refer (lighttable-ops)])
  (import (org.redstonechips.cljchips CljChips clj)
          (org.redstonechips RCPrefs)))

;; Add libray folder to classpath.
(cemerick.pomegranate/add-classpath (CljChips/folder))

(require '[cljchips.command :refer (register-rcx)])
(require '[cljchips.core :refer (rc scheduler)])

;; Setup clj preferences.
(RCPrefs/registerCircuitPreference clj "REPLport" 4555)
(RCPrefs/registerCircuitPreference clj "runREPL" true)
(def REPLport (RCPrefs/getPref "clj.REPLport"))
(def runREPL (RCPrefs/getPref "clj.runREPL"))

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
