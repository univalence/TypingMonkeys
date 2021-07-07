(ns blocks.state-sync
  (:refer-clojure :exclude [swap!])
  (:require [clojure.core :as core]
            [monkey-shell.components.core :as comps]
            [typing-monkeys.utils.misc :as utils]
            [firestore-clj.core :as f]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st])
  (:import (java.util HashMap ArrayList)
           (clojure.lang IDeref IRef)))

; ---- firebase machinery ----

(defonce db
  (f/client-with-creds "data/unsafe.json"))

(defn pull-walk

  "recursively transform a firestore object into clojure data"

  [x]
  (cond
    (fu/ref? x)
    (fu/with-ref x (assoc (pull-walk (f/pull x)) :db/id (f/id x)))
    (fu/query? x)
    (fu/with-ref x (pull-walk (f/pull x)))
    (map? x) (into {} (map (fn [[k v]] [(keyword k) (pull-walk v)]) x))
    (vector? x) (mapv pull-walk x)
    (instance? HashMap x) (pull-walk (into {} x))
    (instance? ArrayList x) (pull-walk (into [] x))
    :else x))

(defn watch!

  "watch a firestore document reference,
   calling the given callback on each changes with document as data"

  [doc-ref callback]
  (st/consume (fn [x]
                (let [data (pull-walk x)]
                  (callback data)))
              (f/->stream doc-ref)))

(defn swap!

  "same as clojure.core/swap!
   and does synchronize the corresponding firestore document as well"

  [atom f & args]
  (println "swap! " atom f args)
  (let [ref (fu/data->ref @atom)
        new-state (apply core/swap! atom f args)]
    (f/set! ref (clojure.walk/stringify-keys new-state))))

(defrecord DBAtom [atom ref watches]
  clojure.lang.IDeref
  (deref [_] @atom)
  clojure.lang.IAtom
  (swap [_ f]
    (let [new-data (f @atom)]
      (f/set! ref (clojure.walk/stringify-keys new-data))
      new-data))
  (swap [_ f x] (core/swap! _ #(f % x)))
  (swap [_ f x y] (core/swap! _ #(f % x y)))
  (swap [_ f x y zs] (core/swap! _ #(apply f % x y zs)))
  IRef
  (addWatch [this key callback]
    (let [watcher (fn watcher-fn [event]
                    (when (contains? @watches key)
                      (when (= :NodeDataChanged (:event-type event))
                        (let [new-value (.deref this)]
                          (callback key this nil new-value)))))]
      (core/swap! watches assoc key watcher)
      this))
  (getWatches [_] @watches)
  (removeWatch [this key] (swap! watches dissoc key) this)
  (setValidator [_ _] nil)
  (getValidator [_] nil)

  )

(defmethod print-method DBAtom [this w]
  (print-method 'DBAtom w))

(defn db-atom [ref]
  (let [a (atom (pull-walk ref))]
    (watch! ref
            (fn [x]
              (when (not= @a x)
                (core/reset! a x))))
    (DBAtom. a ref (atom {}))))

(defn existing-ref?
  [ref]
  (seq (f/pull ref)))

(defn connect-document

  "connect to a firebase document
   returns an atom representing it and synchronized on it
   throws if the document do not exist"

  [id]

  (let [ref (f/doc db id)]
    (assert (existing-ref? ref)
            "non existant document,
             please consider using 'initialize-document")
    (db-atom ref)))



(defn create-document

  "create a firestore document with the given id and data
   returns an atom and setup the synchronisation between it and firestore"

  [id data]

  (let [ref (f/doc db id)]
    (f/set! ref (clojure.walk/stringify-keys data))
    (db-atom ref)))

(defn bind-document

  "the union of connect-document and initialize-document
   if the given id do not point to an existing document,
   default-data is used to create a new one"

  [id default-data]
  (let [ref (f/doc db id)]
    (if (existing-ref? ref)
      (connect-document id)
      (create-document id default-data))))

(comment :exemple
         (def my-data
           (bind-document
             "scratch/my-data"
             {:name "papa" :destination "maman" :score 0}))
         (swap! my-data update :score inc)
         (assert (= 1 (:score @my-data))))

(do :composition

    (comment
      (def m {:a 1 :b {:c 2}})

      {:get (fn [m] (get m :a))
       :set (fn [m f] (update m :a f))}

      {:get (fn [m] (get-in m [:b :c]))
       :set (fn [m f] (update-in m [:b :c] f))})

    (comment :proto-and-records
             (defprotocol IShow
               (show [_]))

             (defrecord Pair [left right]
               IShow
               (show [_] (list 'pair left right)))

             (show (Pair. 1 2))
             ;; => (pair 1 2)
             )

    (defrecord AtomLens [data get set watches #_validator]
      clojure.lang.IDeref
      (deref [_] (get data))
      clojure.lang.IAtom
      (swap [_ f] (->> (get data) f (set data)))
      (swap [_ f x] (core/swap! _ #(f % x)))
      (swap [_ f x y] (core/swap! _ #(f % x y)))
      (swap [_ f x y zs] (core/swap! _ #(apply f % x y zs)))
      IRef
      (addWatch [this key callback]
        (let [watcher (fn watcher-fn [event]
                        (when (contains? @watches key)
                          (when (= :NodeDataChanged (:event-type event))
                            (let [new-value (.deref this)]
                              (callback key this nil new-value)))))]
          (core/swap! watches assoc key watcher)
          this))
      (getWatches [_] @watches)
      (removeWatch [this key] (swap! watches dissoc key) this)
      (setValidator [_ _] nil)
      (getValidator [_] nil)
      )

    (defmethod print-method AtomLens [this w]
      (print-method (list 'atom-lens (:data this)) w))

    (defn atom-lens-class [get set]
      (fn [data]
        (AtomLens. data get set (atom {}))))

    (def ref-map
      (atom-lens-class
        (fn [m] (into {} (map (fn [[k v]] [k (deref v)]) m)))
        (fn [m value]
          (doseq [k (keys m)]
            (reset! (m k) (value k)))
          value)))

    (def c0 (atom {:count 0}))
    (def c1 (atom {:count 1}))
    (def c2 (atom {:count 2}))

    (comment :ex1

             (def gc (ref-map {:c1 c0 :c2 c1 :c3 c2}))

             (assert (= (deref gc)
                        {:c1 {:count 0} :c2 {:count 1} :c3 {:count 2}}))

             (core/swap! gc update-in [:c2 :count] inc)

             (assert (= (deref gc)
                        {:c1 {:count 0}, :c2 {:count 2}, :c3 {:count 2}}))

             (assert (= 2 (:count @c1))))

    (comment :ex2
             (def ref-vec
               (atom-lens-class
                 (fn [v] (mapv deref v))
                 (fn [v value] (mapv #(reset! (get v %) (get value %)) (range (count v))))))

             (ref-vec [c0 c1 c2])

             (assert (= @(ref-vec [c0 c1 c2])
                        [{:count 0} {:count 1} {:count 2}]))

             (assert (= (core/swap! (ref-vec [c0 c1 c2]) update-in [0 :count] inc)
                        [{:count 1} {:count 1} {:count 2}]))
             )

    (defn map-vals
      "(= (map-vals inc {:a 1 :b 2})
          {:a 2 :b 3})"
      [f m]
      (zipmap (keys m) (map f (vals m))))

    (defn atom? [x]
      (instance? clojure.lang.Atom x))

    (def composite-ref
      (atom-lens-class
        (fn self [data]
          (cond
            (map? data) (map-vals self data)
            (vector? data) (mapv self data)
            (atom? data) (deref data)
            :else data))

        (fn self [data value]
          (cond
            (map? data) (merge-with self data value)
            (vector? data) (mapv self data value)
            (atom? data) (reset! data value)
            :else value))))

    (deref (composite-ref 12))

    (comment :ex3
             (def rc (composite-ref
                       {:a c0
                        :b [c1 c2]
                        :c 12}))

             (assert (= (deref rc)
                        {:a {:count 0}
                         :b [{:count 1} {:count 2}]
                         :c 12}))

             (assert (= (core/swap! rc update-in [:b 1 :count] inc)
                        {:a {:count 0}
                         :b [{:count 1} {:count 3}]
                         :c 12}))

             (assert (= {:count 3} @c2))

             (core/swap! c2 update :count inc)

             (deref rc)
             ;; => {:a {:count 0}, :b [{:count 1} {:count 5}], :c 12}
             )

    )

(comment :maybe-useless
         (defn document-id->atom

           "usage: (document-id->atom \"scratch/counter1\")"

           [document-id]
           (-> (f/doc db document-id)
               (pull-walk)
               (atom))))




