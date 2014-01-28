(require '[cljchips.display :refer (make-mem-display
                                                      make-multi-display
                                                      make-font)] :reload)

(on init [args]
    (def displays {:front (assoc (make-mem-display 24 16 "room_front")
                            :location [16 0])
                   :back  (assoc (make-mem-display 24 16 "room_back")
                            :location [56 0])
                   :left  (assoc (make-mem-display 16 16 "room_left")
                            :location [0 0])
                   :right (assoc (make-mem-display 16 16 "room_right")
                            :location [40 0])})
  (def multi (make-multi-display (vals displays)))
  (def font (make-font "font68" 6 8))
  circuit)

(defn animate []
  ((:fill-circle multi) (rand-int (:width multi))
                        (rand-int (:height multi))
                        (rand-nth (range 2 6))
                        (rand-int 15)))

(on input [state idx]
  (case idx
    0 (animate)
    nil))
