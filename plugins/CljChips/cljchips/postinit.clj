(ns cljchips.postinit
  "Anything you write in here gets executed once the server is initalized."
  (require [cljchips.command :refer (rcx)]
           [cljchips.core :refer :all]))

;; Sample command /rcx freezetime [on|off]

(let [task-ref (atom nil)]
  (defn freeze-time [toggle & [time update-freq]]
    (if toggle
      (if-not @task-ref
        (let [update-freq (if update-freq update-freq 3000)
              time (if time time (:noon game-time))
              reset-time (fn [] (set-time time))]
          (swap! task-ref
                 (fn [val]
                   (.runTaskTimer scheduler plugin reset-time 0 update-freq)))
          (broadcast-message (str "Time is now stuck ("
                                  (.getTaskId @task-ref)
                                  ").")))
        (throw (IllegalArgumentException. "Time is already stuck.")))
      (if @task-ref
        (do
          (.cancel @task-ref)
          (broadcast-message (str "Time is unstuck from now on ("
                                  (.getTaskId @task-ref)
                                  ")."))
          (reset! task-ref nil))
        (throw (IllegalArgumentException. "Time was not stuck."))))))

(defmethod rcx "freezetime" [sender args]
  (let [syntax-error #(.sendMessage sender
                                    "Bad syntax. Expecting: /rcx freezetime on/off")]
    (let [[_ toggle] args]
      (if toggle
        (try
          (case toggle
            "on" (freeze-time true)
            "off" (freeze-time false)
            syntax-error)
          (catch IllegalArgumentException e (.sendMessage sender (.getMessage e))))
        (syntax-error)))))

(freeze-time true)
