(ns com.jkkramer.ordered.map
  (:use [com.jkkramer.ordered.common :only [change! Compactable compact]]
        [deftype.delegate :only [delegating-deftype]])
  (:require [clojure.string :as s])
  (:import (clojure.lang IPersistentMap
                         IPersistentCollection
                         IPersistentVector
                         IEditableCollection
                         ITransientMap
                         ITransientVector
                         IObj
                         IFn
                         MapEquivalence
                         Reversible
                         MapEntry
                         ;; Indexed, maybe add later?
                         ;; Sorted almost certainly not accurate
                         )
           (java.util Map Map$Entry)))

(defn entry [k v i]
  (MapEntry. k (MapEntry. i v)))

(declare transient-ordered-map)

(delegating-deftype OrderedMap [^IPersistentMap backing-map
                                ^IPersistentVector order]
  {backing-map {IPersistentMap [(count [])]
                Map [(size [])
                     (containsKey [k])
                     (isEmpty [])
                     (keySet [])]}}
  ;; tagging interfaces
  MapEquivalence

  IPersistentMap
  (equiv [this other]
    (and (instance? Map other)
         (= (.count this) (.size ^Map other))
         (every? (fn [^MapEntry e]
                   (= (.val e) (.get ^Map other (.key e))))
                 (.seq this))))
  (entryAt [this k]
    (when-let [v (.get this k)]
      (MapEntry. k v)))
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (if-let [^MapEntry e (.get ^Map backing-map k)]
      (.val e)
      not-found))

  IFn
  (invoke [this k]
    (.valAt this k))
  (invoke [this k not-found]
    (.valAt this k not-found))

  Map
  (get [this k]
    (.valAt this k))
  (containsValue [this v]
    (boolean (seq (filter #(= % v) (.values this)))))
  (values [this]
    (map (comp val val) (.seq this)))

  Object
  (toString [this]
    (str "{" (s/join ", " (for [[k v] this] (str k " " v))) "}"))
  (equals [this other]
    (.equiv this other))
  (hashCode [this]
    (reduce (fn [acc ^MapEntry e]
              (let [k (.key e), v (.val e)]
                (unchecked-add ^Integer acc ^Integer (bit-xor (hash k) (hash v)))))
            0 (.seq this)))
  IPersistentMap
  (empty [this]
    (OrderedMap. {} []))
  (cons [this obj]
    (condp instance? obj
      Map$Entry (let [^Map$Entry e obj]
                  (.assoc this (.getKey e) (.getValue e)))
      IPersistentVector (if (= 2 (count obj))
                          (.assoc this (nth obj 0) (nth obj 1))
                          (throw (IllegalArgumentException.
                                  "Vector arg to map conj must be a pair")))
      (persistent! (reduce (fn [^ITransientMap m ^Map$Entry e]
                             (.assoc m (.getKey e) (.getValue e)))
                           (transient this)
                           obj))))

  (assoc [this k v]
    (if-let [^MapEntry e (.get ^Map backing-map k)]
      (let [old-v (.val e)]
        (if (= old-v v)
          this
          (let [i (.key e)]
            (OrderedMap. (.cons backing-map (entry k v i))
                         (.assoc order i (MapEntry. k v))))))
      (OrderedMap. (.cons backing-map (entry k v (.count order)))
                   (.cons order (MapEntry. k v)))))
  (without [this k]
    (if-let [^MapEntry e (.get ^Map backing-map k)]
      (OrderedMap. (.without backing-map k)
                   (.assoc order (.key e) nil))
      this))
  (seq [this]
    (seq (keep identity order)))
  (iterator [this]
    (clojure.lang.SeqIterator. (.seq this)))

  IObj
  (meta [this]
    (.meta ^IObj backing-map))
  (withMeta [this m]
    (OrderedMap. (.withMeta ^IObj backing-map m) order))

  IEditableCollection
  (asTransient [this]
    (transient-ordered-map this))

  Reversible
  (rseq [this]
    (seq (keep identity (rseq order))))

  Compactable
  (compact [this]
    (OrderedMap. backing-map
                 (vec (filter identity order)))))

(def ^{:private true,
       :tag OrderedMap} empty-ordered-map (empty (OrderedMap. nil nil)))

(defn ordered-map
  ([] empty-ordered-map)
  ([coll]
     (into empty-ordered-map coll))
  ([k v & more]
     (apply assoc empty-ordered-map k v more)))

;; contains? is broken for transients. we could define a closure around a gensym
;; to use as the not-found argument to a get, but deftype can't be a closure.
;; instead, we pass `this` as the not-found argument and hope nobody makes a
;; transient contain itself.

(delegating-deftype TransientOrderedMap [^{:unsynchronized-mutable true, :tag ITransientMap} backing-map,
                                         ^{:unsynchronized-mutable true, :tag ITransientVector} order]
  {backing-map {ITransientMap [(count [])]}}
  ITransientMap
  (valAt [this k]
    (.valAt this k nil))
  (valAt [this k not-found]
    (if-let [^MapEntry e (.valAt backing-map k)]
      (.val e)
      not-found))
  (assoc [this k v]
    (let [^MapEntry e (.valAt backing-map k this)
          vector-entry (MapEntry. k v)
          i (if (identical? e this)
              (do (change! order .conj vector-entry)
                  (dec (.count order)))
              (let [idx (.key e)]
                (change! order .assoc idx vector-entry)
                idx))]
      (change! backing-map .conj (entry k v i))
      this))
  (conj [this e]
    (let [[k v] e]
      (.assoc this k v)))
  (without [this k]
    (let [^MapEntry e (.valAt backing-map k this)]
      (when-not (identical? e this)
        (let [i (.key e)]
          (change! backing-map dissoc! k)
          (change! order assoc! i nil)))
      this))
  (persistent [this]
    (OrderedMap. (.persistent backing-map)
                 (.persistent order))))

(defn transient-ordered-map [^OrderedMap om]
  (TransientOrderedMap. (.asTransient ^IEditableCollection (.backing-map om))
                        (.asTransient ^IEditableCollection (.order om))))

(defmethod print-method OrderedMap [o ^java.io.Writer w]
  (.write w "#ordered/map ")
  (print-method (seq o) w))
