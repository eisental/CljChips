(ns cljchips.display
  (:require [cljchips.memory :refer :all]))

(defn line
  [point-fn x1 y1 x2 y2 color]
  (let [dx (Math/abs (- x2 x1))
        dy (Math/abs (- y2 y1))
        sx (if (< x1 x2) 1 -1)
        sy (if (< y1 y2) 1 -1)]
    (loop [x1 x1
           y1 y1
           err (- dx dy)]
      (point-fn x1 y1 color)
      (when-not (and (== x1 x2) (== y1 y2))
        (let [e2 (* err 2)]
          (cond
           (> e2 (- dy))
             (recur (+ x1 sx) y1        (- err dy))
           (< e2 dx)
             (recur x1        (+ y1 sy) (+ err dx))
           (and (> e2 (- dy)) (< e2 dx))
             (recur (+ x1 sx) (+ y1 sy) (+ err (- dy) dx))))))))

(defn circle
  [point-fn x0 y0 radius color]
  (let [plot4points
        (fn [x y]
          (point-fn (+ x0 x) (+ y0 y) color)
          (when-not (zero? x)
            (point-fn (- x0 x) (+ y0 y) color))
          (when-not (zero? y)
            (point-fn (+ x0 x) (- y0 y) color))
          (when-not (and (zero? x) (zero? y))
            (point-fn (- x0 x) (- y0 y) color)))

        plot8points
        (fn [x y]
          (plot4points x y)
          (when-not (== x y) (plot4points y x)))]

    (loop [error (- radius)
           x radius
           y 0]
      (when (>= x y)
        (plot8points x y)
        (let [error (+ error y)
              y (inc y)
              error (+ error y)]
          (if (>= error 0)
            (let [error (- error x)
                  x (dec x)
                  error (- error x)]
              (recur error x y))
            (recur error x y)))))))

(defn fill-circle
  [point-fn x0 y0 radius color]
  (let [fill4points
        (fn [x y]
          (line point-fn (- x0 x) (+ y0 y) (+ x0 x) (+ y0 y) color)
          (if (and (not= x 0) (not= y 0))
            (line point-fn (- x0 x) (- y0 y) (+ x0 x) (- y0 y) color)))]

    (loop [error (- radius)
           x radius
           y 0]
      (let [lastY y
            error (+ error y)
            y (inc y)
            error (+ error y)]
        (fill4points x lastY)
        (if (>= error 0)
          (do (if-not (== x lastY) (fill4points lastY x))
              (let [error (- error x)
                    x (dec x)
                    error (- error x)]
                (if (>= x y) (recur error x y))))
          (if (>= x y) (recur error x y)))))))

(defn rect
  [point-fn x1 y1 x2 y2 color]
  (line point-fn x1 y1 x1 y2 color)
  (line point-fn x1 y2 x2 y2 color)
  (line point-fn x2 y2 x2 y1 color)
  (line point-fn x2 y1 x1 y1 color))

(defn fill-rect
  [point-fn x1 y1 x2 y2 color]
  (doseq [x (range x1 (inc x2))
          y (range y1 (inc y2))]
    (point-fn x y color)))

(defn polygon
  [point-fn coords color]
  (let [coords (conj coords (nth coords 0))]
    (loop [i 0]
      (when (< i (dec (count coords)))
        (let [c (nth coords i)
              c+1 (nth coords (inc i))]
          (line point-fn
                (nth c   0) (nth c   1)
                (nth c+1 0) (nth c+1 1)
                color))
        (recur (inc i))))))

(defn text
  [point-fn font string x0 y0 color]
  (let [{width :width height :height get-glyph :get-glyph} font]
    (dotimes [n (count string)]
      (let [char (nth string n)
            char-offset (* n width)
            glyph (get-glyph char)]
        (doseq [i (range (* width height))]
          (when (get glyph i)
            (let [x (rem i width)
                  y (long (/ i width))]
              (point-fn (+ x x0 char-offset) (+ y y0) color))))))))

(defn make-display
  "Returns a display driver map object with drawing functions
   that use the supplied point-fn function. Keeps track of the
   backgroud color that is used when clearing the display."
  [width height point-fn]
  (let [background (atom 0)]
    {:point point-fn
     :clear #(fill-rect point-fn 0 0 width height @background)
     :line (partial line point-fn)
     :circle (partial circle point-fn)
     :fill-circle (partial fill-circle point-fn)
     :rect (partial rect point-fn)
     :fill-rect (partial fill-rect point-fn)
     :polygon (partial polygon point-fn)
     :text (partial text point-fn)
     :set-background (fn [color] (swap! background (fn [_] color)))
     :width width
     :height height
     :background #(deref background)}))

(defn make-mem-display
  "Creates a display driver for memory-backed display circuits."
  [width height mem-id]
  (let [mem (get-mem mem-id)
        point-fn (fn [x y color]
                   (when (and (< x width) (>= x 0) (< y height) (>= y 0))
                     (let [coord (long (+ x (* y width)))]
                       (.write mem coord color))))]

    (assoc (make-display width height point-fn)
      :release (fn [] (.release mem))
      :mem mem)))

(defn make-multi-display [displays]
  "Creates a multi-display driver object consisting of a vector of displays.
   displays are generated using make-display and are expected to have an
   additional :location key with [x y] coordinates of the display offset in
   the multi-display."
  (let [size (reduce
              (fn [dim disp]
                (let [req-width  (+ ((:location disp) 0) (:width disp))
                      req-height (+ ((:location disp) 1) (:height disp))]
                  [(max (dim 0) req-width)
                   (max (dim 1) req-height)]))
              [0 0] displays)
        coord-map (into {}
                   (for [x (range (size 0))
                         y (range (size 1))]
                     [[x y] (filter
                             (fn [disp]
                               (let [xmin ((:location disp) 0)
                                     xmax (+ xmin (:width disp))
                                     ymin ((:location disp) 1)
                                     ymax (+ ymin (:height disp))]
                                 (and (>= x xmin)
                                      (<  x xmax)
                                      (>= y ymin)
                                      (<  y ymax))))
                             displays)]))
        multi-point-fn (fn [x y color]
                         (when-let [displays (coord-map [x y])]
                           (doseq [disp displays]
                             ((:point disp) (- x ((:location disp) 0))
                                            (- y ((:location disp) 1))
                                            color))))]
    (assoc (make-display (size 0) (size 1) multi-point-fn)
      :displays displays
      :coord-map coord-map)))

(defn make-font [mem-id width height]
  (let [mem (get-mem mem-id)]
    {:mem mem
     :width width
     :height height
     :release #(.release mem)
     :get-glyph (fn [ascii-char]
                  (.read mem (int ascii-char)))}))
