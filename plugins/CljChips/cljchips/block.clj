(ns cljchips.block
  (require [cljchips.core :refer (server)])
  (import (org.bukkit Material Location)
          (org.bukkit.block Block)
          (org.bukkit.material MaterialData)))

;; Transform Location

(defn loc->map [^Location loc]
  "Transforms a Location object into a block loc map with :world, :x, :y and :z keys."
  {:world (-> loc .getWorld .getName)
   :x (.getBlockX loc)
   :y (.getBlockY loc)
   :z (.getBlockZ loc)})

(defn map->loc [map]
  "Transforms a loc map with :world, :x, :y and :z keys to a Location object"
  (when-let [world (.getWorld server (:world map))]
    (Location. world (:x map) (:y map) (:z map))))

(defn relative-loc [loc root]
  "Returns a loc map with coordinates relative to the root loc map."
  (-> loc
      (dissoc :world)
      (assoc :x (- (:x loc) (:x root)))
      (assoc :y (- (:y loc) (:y root)))
      (assoc :z (- (:z loc) (:z root)))))

(defn relative-to [rel-loc loc]
  "Transforms a relative loc map into an absolute loc map in relation to loc."
  (-> rel-loc
      (assoc :world (:world loc))
      (assoc :x (+ (:x loc) (:x rel-loc)))
      (assoc :y (+ (:y loc) (:y rel-loc)))
      (assoc :z (+ (:z loc) (:z rel-loc)))))

;; Transform MaterialData

(defn material->map [^MaterialData md]
  "Transforms a MaterialData object into a map with :type and :data keys."
  {:type (.getItemTypeId md)
   :data (.getData md)})

(defn map->material [map]
  "Transforms a material map with :type and :data keys into a MaterialData object."
  (MaterialData. ^int (:type map) ^byte (:data map)))

(defn get-material [^Location loc]
  "Returns the MaterialData object of the block at location loc."
  (-> loc .getBlock .getState .getData))

;; Block state

(defn block-state [^Location loc] (-> loc .getBlock .getState))

(defn alter-block [^Location loc f]
  (let [state (block-state loc)]
    (f state)
    (.update state true)))

(defn alter-type [^Location loc ^Material m]
  (alter-block loc #(.setType % m)))

(defn alter-material-data [loc m]
  (alter-block
   (map->loc loc)
   (fn [s]
     (.setTypeId s (:type m ))
     (.update s true)
     (.setRawData s (:data m)))))
