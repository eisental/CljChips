(import (org.redstonechips CircuitLoader)
        (org.redstonechips.chip.io IOWriter))

(defn init-circuit [c args]
  (set! (.activator c) ($ activator))
  (let [init-c (.init c args)]
    (set! (.activator c) nil)
    init-c))

(defn make-circuit [type args inputlen outputlen io-writer]
  (let [args (into-array String args)
        c (doto (CircuitLoader/getCircuitInstance type)
          (.constructWith
           ($ chip)
           (reify IOWriter
             (writeOut [this state index]
               (io-writer state index)))
           inputlen
           outputlen))]
    (loop [c1 c
           c2 (init-circuit c1 args)]
      (cond (nil? c2) nil
            (= c1 c2) c2
            :else (recur c2 (init-circuit c2 args))))))

(defn out-writer [state index]
  ($ write state index))

(defn make-chain-writer [output-circuit]
  (fn [state index] (.input output-circuit state index)))

(on init [args]
  (def counter (make-circuit "counter" [] 1 4 out-writer))
  (def clock (make-circuit "clock" ["100ms" "0.5"] 1 1 (make-chain-writer counter)))
  circuit)

(on input [state idx]
  (aset (.inputs clock) idx state)
  (.input clock state idx))

(on shutdown []
  (.shutdown counter)
  (.shutdown clock))


;; - Call all possible methods of nested circuits.
;; - Make complex routes between io pins.
;; - Create a graph from a definition map.
;; - Write DSL macro to generate definition maps.



#_ "(circuit-graph
   [clk (clock [100ms 0.5] 1 1)
    cnt1 (counter [] 1 4)
    cnt2 (counter [] 1 4)]
   (->
    (0 clock
       (0 counter
          (0..4 ->))
       (1 0 counter
          (0 synth)))))
"
