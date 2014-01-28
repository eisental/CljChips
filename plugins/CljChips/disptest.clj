(require '[cljchips.display :as disp] :reload
         '[clojure.repl :refer (doc find-doc)])

(declare d straight-travel font6x8)

(defn rainbow [x y i]
  ((d :clear))
  (dotimes [n  (dec i)]
    ((d :fill-circle) x y (- i n) n)))

(defn xormap []
  (doseq [x (range 16) y (range 16)]
    ((d :point) x y (bit-xor x y))))

(defn linetest []
  (doseq [n (range 16)]
    ((d :line) 0 0 15 n (rem n 16))))

(defn make-straight-travel [width height color]
  (let [coords (vec (for [x (range width)
                          y (range height)] [x y]))
        n (atom 0)]
    (fn []
      (if (zero? @n)
        ((d :clear))
        (let [last-coord (coords (dec @n))]
          ((d :point) (last-coord 0) (last-coord 1) 0)))
      (apply (d :point) (conj (coords @n) color))
      (if (>= @n (dec (count coords)))
        (reset! n 0)
        (swap! n inc)))))

(on input [state idx]
    (when (and state (== idx 0)) (straight-travel)))

(on shutdown []
    (when d ((:release d)))
    (when font6x8 ((:release font6x8))))

(on init [args]
    (def width (read-string (nth args 1)))
    (def height (read-string (nth args 2)))
    (def mem-id (nth args 3))
    (def font6x8 (disp/make-font "font68" 6 8))
    (def d (disp/make-mem-display width height mem-id))
    (def straight-travel (make-straight-travel width height (rand-int 16)))
    circuit)
