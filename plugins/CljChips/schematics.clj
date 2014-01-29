(ns schematics
  (require [cljchips.core :refer :all] :reload
           [cljchips.command :refer (rcx)]
           [cljchips.block :refer :all] :reload)
  (import (org.redstonechips RCPrefs)
          (org.redstonechips.chip Chip)
          (org.redstonechips.chip.io OutputPin)
          (org.redstonechips.util Signs)
          (org.redstonechips.command CommandUtils)
          (java.io File IOException)))

#_(TODO ["rename everything to schematics instead of templates"
         "undo/redo"
         "save template as string in player data perhaps with a name."
         "template rotation based on player direction."
         "only change air. stop when encountering hard substances."
         "load straight to blocks (w/o storing in session)?"
         "Check for errors when activating chips."])

;; Default IO block types

(defn input-block-type [] (material->map (RCPrefs/getInputBlockType)))
(defn output-block-type [] (material->map (RCPrefs/getOutputBlockType)))
(defn interface-block-type [] (material->map (RCPrefs/getInterfaceBlockType)))

;; Templates

(defn get-output-blocks [chip]
  (reduce #(into %1 (.getOutputBlocks %2)) [] (.outputPins chip)))

(defn find-devices [^Chip chip relative-to]
  (let [devices (filter #(contains? (set OutputPin/deviceMaterials)
                                    (-> % .getBlock .getType))
                        (get-output-blocks chip))]
    (map
     #(into (relative-to %) (material->map (get-material  %)))
     devices)))

(defn args->array [args]
  (let [array (make-array String (count args))]
    (doseq [i (range (count args))]
      (let [arg (nth args i)]
        (aset array i arg)))
    array))

(defn make-template
  "Creates a template from chip. Locations are stored relative to :root argument
   if used or to the chip activation block otherwise."
  [^Chip chip & {:keys [root] :or {root (loc->map (.activationBlock chip))}}]
  (let [relative-to-root #(relative-loc (loc->map %) root)]
    {:type       (.getType chip)
     :args       (vec (aclone (.args chip)))
     :structure  (map #(-> %
                           (relative-to-root)
                           (into (material->map (get-material %))))
                      (.structure chip))
     :inputs     (map #(relative-to-root (.getLocation %)) (.inputPins chip))
     :outputs    (map #(relative-to-root (.getLocation %)) (.outputPins chip))
     :interfaces (map #(relative-to-root (.getLocation %)) (.interfaceBlocks chip))
     :material   (material->map (get-material (nth (.structure chip) 1)))
     :sign       (-> (relative-to-root (.activationBlock chip))
                     (into (material->map (get-material (.activationBlock chip)))))
     :devices    (find-devices chip relative-to-root)}))

(defn build-chip [template root]
  (doseq [struct (map #(relative-to % root) (:structure template))]
    (alter-material-data struct struct))
  (doseq [input (map #(relative-to % root) (:inputs template))]
    (alter-material-data input (input-block-type)))
  (doseq [output (map #(relative-to % root) (:outputs template))]
    (alter-material-data output (output-block-type)))
  (doseq [interface (map #(relative-to % root) (:interfaces template))]
    (alter-material-data interface (interface-block-type)))
  (doseq [device (map #(relative-to % root) (:devices template))]
    (alter-material-data device device))
  (let [sign-loc (relative-to (:sign template) root)]
    (alter-material-data sign-loc (:sign template))
    (Signs/writeChipSign (block-state (map->loc sign-loc)) (:type template) (args->array (:args template)))
    sign-loc))

;; Copy/Paste/Activate functions

(defn activate-chip
  "Try to activate a chip starting with the chip sign.
   sign-loc: loc map of the chip sign.
   cs:       activator."
  [sign-loc cs]
  (.maybeScanChip chipManager (.getBlock (map->loc sign-loc)) cs -1))

(defn copy-selection
  "Creates templates from each chip in player /rcsel selection, relative to the player's location."
  [player]
  (let [root (loc->map (.getLocation player))
        session (.getUserSession rc player true)
        selection (.getSelection session)]
    (map #(make-template % :root root) selection)))

(defn copy-target-chip
  "Makes a template out of the chip the player is looking at."
  [player]
  (when-let [chip (CommandUtils/findTargetChip player)]
    [(make-template chip :root (loc->map (.getLocation player)))]))

(defn paste
  "Builds chips from templates in coll relative to root location.
   When `:activate? true`, all chips are activated."
  [root coll & {:keys [activate? player] :or {activate? false player nil}}]
  (on-server-thread
   (fn []
     (if activate?
       (doseq [template coll]
         (activate-chip (build-chip template root) player))
       (doseq [template coll]
         (build-chip template root))))))

(defn- put-in-session
  "Stores a collection of chips in player session data under 'template'"
  [player template]
  (when template
    (let [session (.getUserSession rc player true)]
      (.putPlayerData session "template" template)
      (.sendMessage player (str "Stored " (count template) " chip(s) as template.")))))

(defn grab-from-session
  "Return template stored as data in the player session."
  [player]
  (let [no-template #(.sendMessage player "You don't have a template stored.")]
    (if-let [session (.getUserSession rc player false)]
      (if-let [template (.getPlayerData session "template")]
        template
        (no-template))
      (no-template))))

(defn paste-from-session
  "Pastes chips from data in player session."
  [player]
  (when-let [template (grab-from-session player)]
    (let [root (loc->map (.getLocation player))]
      (paste root template :activate? true :player player))))

;; Template files

(defn find-or-make-folder []
  (let [rc-folder (.getDataFolder rc)
        template-folder (File. rc-folder "templates")]
    (if-not (.exists template-folder) (.mkdir template-folder))
    template-folder))

(defn filename-for [name]
  (.getPath (File. (find-or-make-folder) (str name ".tmpl"))))

;; Save templates

(defn save-template
  "Saves a template to file."
  [name template]
  (spit (filename-for name) (with-out-str (pr template))))

(defn save-from-session
  "Save template stored in player session."
  [player name]
  (if name
    (when-let [template (grab-from-session player)]
      (save-template name template)
      (.sendMessage player (str "Saved template to " (filename-for name))))
    (.sendMessage player "Expecting a template name.")))

;; Load templates

(defn load-template
  "Load template from file by name."
  [name]
  (let [filename (filename-for name)]
    (read-string (slurp filename))))

(defn load-into-session
  "Load template into player session."
  [player name]
  (if name
    (try
      (let [template (load-template name)]
        (put-in-session player template)
        (.sendMessage player "Loaded template. Use `/rcx tmpl paste` to paste it."))
      (catch IOException e
        (.sendMessage player (str "Can't load template: " (.getMessage e)))))
    (.sendMessage player "Expecting a template name.")))

;; `/rcx tmpl` Command
;;
;; /rcx tmpl copy        -> copy target chip, put template in player data.
;; /rcx tmpl copysel     -> copy /rcsel selection in player session. put template
;;                          in player data.
;; /rcx tmpl paste       -> paste templates relative to commandsender location
;; /rcx tmpl save <name> -> save session template to file.
;; /rcx tmpl load <name> -> load template from file into session.

(defmethod rcx "tmpl" [cs args]
  (let [syntaxError #(.sendMessage cs "Invalid syntax.")]
    (when-let [player (CommandUtils/enforceIsPlayer cs)]
      (if-let [cmd (get args 1)]
        (case cmd
          "copy" (put-in-session player (copy-target-chip player))
          "copysel" (put-in-session player (copy-selection player))
          "paste" (paste-from-session player)
          "save" (save-from-session player (get args 2))
          "load" (load-into-session player (get args 2)))
        (syntaxError)))))
