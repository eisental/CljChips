(ns cljchips.command
  (import (org.redstonechips.cljchips RcxDispatcher RcxCommand)))

(defmulti rcx (fn [_ args] (first args)))

(defmethod rcx :default [cs args]
  (.sendMessage cs (str "No registered command for `/rcx " (first args) "`.")))

(defn register-rcx
  []
  (set! RcxDispatcher/command
        (reify RcxCommand
          (execute [this cs args]
            (rcx cs args)))))
