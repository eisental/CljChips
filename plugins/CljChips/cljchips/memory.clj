(ns cljchips.memory
  (import (org.redstonechips.memory Memory Ram)))

(defn get-mem [id]
  (Memory/getMemory id Ram))

(defn anonymous-mem []
  (Memory/getAnonymousMemory Ram))
