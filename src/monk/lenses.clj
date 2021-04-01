(ns monk.lenses
  (:refer-clojure :exclude [< get =])
  (:require [clojure.core :as c]
            [monk.vec :as vec]
            [monk.utils :as u :refer [defn+ marked-fn]])
  (:import (clojure.lang Keyword PersistentVector IPersistentMap Fn)))

;; Lens
;; -----------------------------------------------------------

(declare lens builtins id)

(defrecord Lens [get upd])

(defprotocol ILens
  :extend-via-metadata true
  (->lens [x]))

(defn lensable [data get upd]
  (vary-meta data assoc `->lens (Lens. get upd)))

(defn lens? [x]
  (cond (instance? Lens x) x
        (satisfies? ILens x) (->lens x)))


(marked-fn lfn
           "'lfn works exactly like fn but
            return a function that is marked with metadata
            an lfn can be recognized via the lfn? predicate
            lfn(s) behaves differently that regular lambdas when used as a lens
            the value that it returns is persisted during navigation
            it can be used as/in a lens that performs coercion (see exemples)
            it also has a special behavior on arity 1,
            that can be used to turn a regular function to a 'lfn")

;; treating all fns as lfns could have been considered
;; introducing a function to wrap predicates into guards
;; the following form (valid in the current version)
;; (upd {:a 1} [:a number? pos? (lfn inc)] identity)
;; could have been replaced by one of those two forms
;; (upd {:a 1} [:a (guard number?) (guard pos?) inc] identity)
;; (upd {:a 1} [:a (guards number? pos?) inc] identity)

;; operations
;; -----------------------------------------------------------

(defn+ get
       ([x l]
        ((:get (lens l)) x))
       ([x l & ls]
        (reduce get (get x l) ls)))

(defn+ upd
       ([x l f]
        ((:upd (lens l)) x f))
       ([x l f & lfs]
        (reduce upd*
                (upd x l f)
                (partition 2 lfs))))

(defn+ upd<
       "takes a datastructure to transform (x)
        and series of couples of form [lens(able) fn]
        executing the first transformation which associated lens does not focuses on nil"
       ([x couples]
        (when (seq couples)
          (or (and (get x (ffirst couples))
                   (upd* x (first couples)))
              (recur x (next couples)))))
       ([x l f & lfs]
        (upd< x (cons [l f] (partition 2 lfs)))))

(defn+ put
       ([x l v]
        (upd x l (constantly v)))
       ([x l v & lvs]
        (reduce put*
                (put x l v)
                (partition 2 lvs))))

(defn+ pass
       "take a datastructure and a series of lenses
        try to forward x thru all given lenses
        can be used to do validation and coercion (with the help of 'lfn)"
       [x & xs]
       (if (seq xs)
         (when-let [x' (upd x (first xs) identity)]
           (pass* x' (next xs)))
         x))

;; creation
;; -----------------------------------------------------------

(defn lens+
  "lens composition, don't use directly
   prefer passing a vector to the 'lens function"
  [l m]
  (Lens. (fn [x] (some-> x (get l) (get m)))
         (fn [x f]
           ;; the idea here is to shortcircuit when encountering an intermediate nil result
           (when-let [v1 (get x l)]
             (when-let [v2 (upd v1 m f)]
               (put x l v2)))
           #_(put x l (upd (get x l) m f)))))

(extend-protocol ILens
  nil
  (->lens [_] (lens identity
                    (fn [x f] (f x))))
  Keyword
  (->lens [x]
    (lens (fn [y] (c/get y x))
          (fn [y f] (update-in y (u/path x) f))))

  Integer
  (->lens [x]
    (lens (fn [y] (vec/get y x))
          (fn [y f] (vec/upd y x f))))

  PersistentVector
  (->lens [x] (reduce lens+ (map lens x)))

  IPersistentMap
  (->lens [m]
    (let [m' (zipmap (keys m) (map lens (vals m)))
          getter
          #(reduce (fn [a e]
                     (let [k (key e)
                           v (get % (val e))]
                       (if v (assoc a k v) (reduced nil))))
                   {} m')]
      (lens getter
            (fn [y f] (f (getter y))))))
  Fn
  (->lens [x]
    (if (lfn? x)
      (lens (fn [y] (x y))
            (fn [y f] (when-let [y' (x y)] (f y'))))
      (lens (fn [y] (when (x y) y))
            (fn [y f] (when (x y) (f y))))))
  Object
  (->lens [x] (lens (partial c/= x))))

(defn lens
  "arity 1:
    convert something to a lens
   arity 2:
    Given a function for getting the focused value from a state
    (getter) and a function that takes the state and and update
    function (setter), constructs a lens."
  ([x]
   #_(println "lens " x)
   (or (lens? x)
       (builtins x)
       (cond

         ;; nil is the identity lens
         (nil? x)
         (lens identity
               (fn [x f] (f x)))

         ;; index or key lens
         ;; TODO handle negative indexes
         (or (keyword? x) (integer? x))
         (lens (fn [y] (c/get y x))
               (fn [y f] (when (contains? y x)
                           (c/update y x f))))

         ;; lens composition
         (vector? x)
         (reduce lens+ (map lens x))

         ;; lfn perform a transformation while navigating
         (lfn? x)
         (lens (fn [y] (x y))
               (fn [y f] (when-let [y' (x y)] (f y'))))

         ;; functions are turned into a guard-lens
         (fn? x)
         (lens (fn [y] (when (x y) y))
               (fn [y f] (when (x y) (f y))))

         ;; values acts as = guards
         :else
         (lens (partial c/= x)))))

  ([get upd]
   (Lens. get upd)))

;; instances and constructors
;; -----------------------------------------------------------

(def builtins
  {:*
   (lens (fn [x] (when (map? x) (vals x)))
         (fn [x f] (u/map-vals f x)))})

(def k
  "Constant lens"
  (lens identity (fn [x _] x)))

(def id
  "Identity lens."
  (lens identity (fn [x f] (f x))))

(def prob
  "probing lens."
  (lens (partial u/pp 'get)
        (fn [x f] (u/pp 'set (f x)))))

(def never
  (lens (constantly nil)
        (constantly nil)))

(defn <
  "fork lens, tries every given lens(able) in order
   use the first that does not focuses on nil"
  [& xs]
  (let [lenses (map lens xs)]
    (lens
     (fn [x]
       (loop [xs lenses]
         (when (seq xs)
           (or (get x (first xs))
               (recur (next xs))))))
     (fn [x f]
       (loop [xs lenses]
         (when (seq xs)
           (if (get x (first xs))
             (upd x (first xs) f)
             (recur (next xs)))))))))

(defn path
  "a lens representing deep access/update in a map (with keyword keys)
   unlike regular lens composition it does not return nil if the path points to nil
   this way it can be used to introduce new values in a map (unlike lens composition, that would have failed (returning nil))"
  [& xs]
  (let [ks (flatten xs)]
    (assert (every? keyword? ks)
            "path should only contains keywords")
    (Lens. (fn [x] (c/get-in x ks))
           (fn [x f] (c/update-in x ks f)))))

(defn ?
  "build a lens that when focuses on nil, returns the state unchanged, or behave normally"
  [l] (< (lens l) k))

(defn !
  "a lens that that returns nil when focuses on nil"
  [l] (< (lens l) never))

(defn convertion
  "Given a function from A to B and another in the
   opposite direction, construct a lens that focuses and updates
   a converted value."
  [one->other other->one]
  (lens one->other
        (fn [s f]
          (other->one (f (one->other s))))))

(defn = [x]
  (lens (fn [y] (when (c/= x y) y))
        (fn [y f] (when (c/= x y) (f y)))))

;; assertions
;; -----------------------------------------------------------

(comment
 (c/= {:a 1 :b "a"}
      (get {:a 1 :b "a"}
           {:a [:a number?] :b [:b string?]}))

 (c/= {:a 1 :b "a"}
      (get {:a 1 :b "a" :c 42}
           {:a number? :b string?}))

 (get (range 10)
      {:cnt (lfn count) :sum (lfn [x] (reduce + x))}
      (lfn [{:keys [cnt sum]}] (/ sum cnt)))

 (nil? (get {:a 1 :b "a"}
            {:a number? :b number?}))

 (get 1 number?))




