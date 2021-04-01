(ns monk.vec
  (:refer-clojure :exclude [type cat butlast last get drop take map rem assoc merge-with])
  (:require [clojure.core :as c]))

(do :indexes

    ;; there is to kind of positions we have to deal with when working on sequential datastructures
    ;; the most common is what I will call here an 'element-idx', it simply points to an actual element of the data structure
    ;; a negative element-idx means that we start from the last element of the sequence
    ;; -1 points to the last element, -2 to the one before it etc...
    ;; the second type of positions we have to deal with those between two elements
    ;; I will call those: 'splitting-idx'
    ;; We are typically using those to insert things between two element or to split a sequence

    (defn element-idx
      "build a valid numeric index for a given vector
       handles negative index"
      [v i]
      (let [c (count v)]
        (cond
          (neg? i) (let [i (c/+ c i)] (if-not (neg? i) i))
          (c/< i c) i)))

    (defn splitting-idx
      "build an splitting-index for a given vector
       an splitting-index is a position between two elements of a vector
       handles negative index"
      [v i]
      (let [c (count v)]
        (cond
          (neg? i) (let [i (c/+ c (inc i))] (if-not (neg? i) i))
          (c/<= i c) i))))

(def last peek)
(def butlast pop)

(defn get
  [v at]
  (c/get v (element-idx v at)))

(declare drop)

(defn cat [& xs]
  (reduce into [] xs))

(defn take [v n]
  (when-let [i (splitting-idx v n)]
    (subvec v 0 i)))

(defn drop [v n]
  (when-let [i (splitting-idx v n)]
    (subvec v i)))

(defn every [v f]
  (loop [ret [] v v]
    (if (seq v)
      (when-let [v1 (f (first v))]
        (recur (conj ret v1) (rest v)))
      ret)))

(defn split [v n]
  [(take v n) (drop v n)])

(defn splits [v idxs]
  (loop [v v at 0
         ret []
         [i & idxs] (sort (c/map #(splitting-idx v %) idxs))]
    (if i
      (when-let [[v1 v2] (split v (c/- i at))]
        (recur v2 i (conj ret v1) idxs))
      (conj ret v))))

(defn span [v [from to]]
  (let [i (splitting-idx v from)]
    (take (drop v i)
          (c/- (splitting-idx v to) i))))

(defn insert
  [v at x]
  (let [[v1 v2] (split v at)]
    (-> (conj v1 x) (into v2))))

(defn splice
  [v at x]
  (let [[v1 v2] (split v at)]
    (-> (into v1 x) (into v2))))

(defn rem [v at]
  (into (take v at) (drop v (inc at))))

(defn put [v at x]
  (when-let [i (element-idx v at)]
    (c/assoc v i x)))

(defn upd [v at f]
  (when-let [i (element-idx v at)]
    (c/update v i f)))

(defn- default-merger [f]
  (fn [x y]
    (cond (not x) y
          (not y) x
          :else (f x y))))

(defn merge-with
  ([x y f]
   (merge-with x y (default-merger f) nil))
  ([x y f placeholder]
   (let [cx (count x)
         cy (count y)]
     (case (compare cx cy)
       0 (mapv f x y)
       1 (mapv f x (into y (repeat cx placeholder)))
       -1 (mapv f (into x (repeat cy placeholder)) y)))))

(defn compress [v bs f]
  (when-let [[a b c] (splits v bs)]
    (-> (conj a (f b)) (into c))))

(defn unconj [v]
  [(butlast v) (last v)])

#_(do :tries

      (is 1
          (get [1 2 3] 0)
          (get [1 2 3] -3))

      (is 2
          (get [1 2 3] 1)
          (get [1 2 3] -2))

      (is 3
          (get [1 2 3] 2)
          (get [1 2 3] -1))

      (is (not (get [1 2 3] 4))
          (not (get [1 2 3] -4)))

      (is [:a 2 3]
          (put [1 2 3] 0 :a))

      (is (not (put [1 2 3] 4 :a)))
      (is (not (put [1 2 3] -4 :a)))

      (is [1 :a 3]
          (put [1 2 3] 1 :a)
          (put [1 2 3] -2 :a))


      (is [1 2]
          (take [1 2 3] 2)
          (take [1 2 3] -2))
      (is [1 2 3]
          (take [1 2 3] 3)
          (drop [1 2 3] 0)
          (take [1 2 3] -1))
      (is [3]
          (drop [1 2 3] 2)
          (drop [1 2 3] -2))
      (is []
          (drop [1 2 3] 3)
          (take [1 2 3] 0)
          (drop [1 2 3] -1))

      (split [1 2 3] 0)
      (split [1 2 3] 1)
      (split [1 2 3] -1)
      (split [1 2 3] -3)
      #_(split [1 2 3] 5)


      (is [3 4] (span [1 2 3 4 5 6] [2 4]))
      (is [2 3 4 5] (span [1 2 3 4 5 6] [1 -2]))

      (splits [1 2 3 4 5 6 7] [1 3 -2])

      (insert [1 2 3] 1 :iop)
      (insert [1 2 3] -1 :iop)
      (insert [1 2 3] -2 :iop)
      (insert [1 2 3] -2 :iop)
      #_(insert [1 2 3] 12 :iop)

      (rem [1 2 3] 1)
      (rem [1 2 3] 10)
      (rem [1 2 3] -1)
      (rem [1 2 3] -2)
      (rem [1 2 3] 0)

      (unconj [1 2 3])

      (merge-with [1 2 3] [4 3] + 0)
      (merge-with [1 2 3] [4 3] +))