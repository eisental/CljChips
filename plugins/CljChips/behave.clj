;;; behave.clj - This example circuit shows how to draw stuff and
;;;              communicate with a display chip.

(require '[cljchips.display :as display])

;; Global vars
;;  disp - The display driver, encapsulating drawing functions and
;;         memory access.
;;  font - Font used for displaying text on the display. Uses an RC
;;         memory for mapping ascii codes to 6x8 bitmaps.
(declare disp font)

(defn animate
  "Renders an animation frame. Called each time the 1st input pin
   is triggered. For a continuous animation, trigger with a clock
   chip setup with sign args `20fps 0`"
  []
  ((:point disp) (rand-int (:width disp))
                 (rand-int (:height disp))
                 (rand-int 16)))
(defn reset []
  "Resets the simulation stated. Called when the 2nd input pin is
   triggered."
  ((:clear disp)))

;; Circuit initalization hook. Called when the chip is activated.
;; Parses sign arguments, initalizes the display driver and font,
;; clears the display and prints "OK" in the middle of the screen.
;;
;; init should return `circuit` when everything goes well, otherwise
;; the chip won't activate.
(on init [args]
  (let [[_ swidth sheight mem-id] args
        args-error #($ error "Expecting args: <width> <height> <mem-id>")]
    (if (and swidth sheight mem-id)
      (let [width (read-string swidth)
            height (read-string sheight)]
        (if (and (number? width) (number? height))
          (do
            (def disp (display/make-mem-display width height mem-id))
            (def font (display/make-font "font68" 6 8))
            ((:clear disp))
            ((:text disp) font "OK"
                          (- (/ width 2) 6)
                          (- (/ height 2) 4) 7)
            circuit)
          (args-error)))
      (args-error))))

;; Circuit input hook. Called when an input pin changes state.
;; Invokes animate or reset, depending on the pin index number.
(on input [state idx]
    (when state
      (case idx
        0 (animate)
        1 (reset))))
