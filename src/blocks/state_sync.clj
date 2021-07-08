(ns blocks.state-sync
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

(comment
  (def a1 (atom {:a 1}))

  (def dba1
    (DBAtom. a1
             (f/doc db "scratch/a1")
             (atom {})))

  (add-watch dba1 :watch-dba1
             (fn [k r old new]
               (println k r old new)))

  (.getWatches (atom 1))

  (deref dba1)
  (swap! dba1 update :a inc))

(defrecord DBAtom [atom ref]
  clojure.lang.IDeref
  (deref [_] @atom)
  clojure.lang.IAtom
  (reset [this v] (swap! this (constantly v)))
  (swap [_ f]
    (let [new-data (swap! atom f)]
      (f/set! ref (clojure.walk/stringify-keys new-data))
      new-data))
  (swap [_ f x] (core/swap! _ #(f % x)))
  (swap [_ f x y] (core/swap! _ #(f % x y)))
  (swap [_ f x y zs] (core/swap! _ #(apply f % x y zs)))
  IRef
  (addWatch [this key callback]
    (.addWatch atom key callback) this)
  (getWatches [_] (.getWatches atom))
  (removeWatch [this key] (remove-watch atom key) this)
  (setValidator [_ x] (.setValidator atom x))
  (getValidator [_] (.getValidator atom))

  )

(defmethod print-method DBAtom [this w]
  (print-method 'DBAtom w))

(defn db-atom [ref]
  (let [a (atom (pull-walk ref))]
    (watch! ref
            (fn [x]
              (when (not= @a x)
                (core/reset! a x))))
    (DBAtom. a ref)))

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

    (comment :atom-lens_deprecated

        (defrecord AtomLens [structure get set watches]
          clojure.lang.IDeref
          (deref [_] (get structure))
          clojure.lang.IAtom
          (reset [_ v] (set v))
          (swap [_ f] (->> (get structure) f (set structure)))
          (swap [_ f x] (core/swap! _ #(f % x)))
          (swap [_ f x y] (core/swap! _ #(f % x y)))
          (swap [_ f x y zs] (core/swap! _ #(apply f % x y zs)))
          IRef
          (addWatch [this key callback]
            (let [watcher (fn watcher-fn [event]
                            (when (contains? @watches key)
                              (let [new-value (.deref this)]
                                (callback key this nil new-value))))]
              (core/swap! watches assoc key watcher)
              this))
          (getWatches [_] @watches)
          (removeWatch [this key] (swap! watches dissoc key) this)
          (setValidator [_ _] nil)
          (getValidator [_] nil)
          )

        (defmethod print-method AtomLens [this w]
          (print-method (list 'atom-lens (:structure this)) w))

        (defn atom-lens-class [get set]
          (fn [structure]
            (AtomLens. structure get set (atom {}))))

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

        (defn atom-lens-add-watch [this k f]
          (letfn [(looop [x]
                    (cond
                      (map? x) (map-vals looop x)
                      (vector? x) (mapv looop x)
                      (atom? x) (add-watch x k (fn [_ _ _ _] (f k this nil (deref this))))
                      :else x))]
            (looop (:structure this))
            this))

        (def composite-ref
          (atom-lens-class
            (fn self [struct]
              (cond
                (map? struct) (map-vals self struct)
                (vector? struct) (mapv self struct)
                (atom? struct) (deref struct)
                :else struct))

            (fn self [struct value]
              (cond
                (map? struct) (merge-with self struct value)
                (vector? struct) (mapv self struct value)
                (atom? struct) (reset! struct value)
                :else value))

            ))

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
                 ))

    (do :catom

        (defn map-vals
          "(= (map-vals inc {:a 1 :b 2})
              {:a 2 :b 3})"
          [f m]
          (zipmap (keys m) (map f (vals m))))

        (defn notify-watches
          [this watches v newv]
          (doseq [[k f] watches]
            (try
              (f k this v newv)
              (catch Exception e
                (throw (RuntimeException. e))))))

        (declare atom?)

        (defn catom_deref [struct]
          (cond
            (atom? struct) (deref struct)
            (map? struct) (map-vals catom_deref struct)
            (vector? struct) (mapv catom_deref struct)
            :else struct))

        (defn catom_reset! [struct value]

          (cond
            (atom? struct) (reset! struct value)
            (map? struct) (merge-with catom_reset! struct value)
            (vector? struct) (mapv catom_reset! struct value)
            :else value))

        (defn catom_dosub
          [x f]
          (cond
            (atom? x) (f x)
            (map? x) (doseq [v (vals x)] (catom_dosub v f))
            (vector? x) (doseq [v x] (catom_dosub v f))
            ))

        (defn catom_hook-subs
          "attach watches to every sub ref of a catom
           in order to propagate to catom level watches"
          [{:as this :keys [structure watches]}]
          (catom_dosub structure
                       (fn [ref] (add-watch ref
                                            (gensym)
                                            (fn [k subref o n]
                                              (notify-watches this
                                                              @watches
                                                              [:subchange [k subref o n]]
                                                              (deref this))))))
          this)

        (defrecord Catom [structure watches validator]
          clojure.lang.IDeref
          (deref [_] (catom_deref structure))
          clojure.lang.IAtom
          (reset [_ v] (set v))
          (swap [this f]
            (let [v (catom_deref structure)
                  nv (->> (f v) (catom_reset! structure))]
              (notify-watches this @watches v nv)
              nv))
          (swap [_ f x] (core/swap! _ #(f % x)))
          (swap [_ f x y] (core/swap! _ #(f % x y)))
          (swap [_ f x y zs] (core/swap! _ #(apply f % x y zs)))
          clojure.lang.IRef
          (setValidator [_ vf] (core/reset! validator vf))
          (getValidator [_] @validator)
          (getWatches [_] @watches)
          (addWatch [a k f] (do (core/swap! watches assoc k f) a))
          (removeWatch [a k] (do (core/swap! watches dissoc k) a)))

        (defmethod print-method Catom [this w]
          (print-method (list 'catom (deref this)) w))

        (defn atom? [x]
          (or (instance? DBAtom x)
              (instance? Catom x)
              (instance? clojure.lang.Atom x)))

        (defn catom [structure]
          (catom_hook-subs
            (Catom. structure (atom {}) (atom identity))))

        (comment :ex1

                 (def c1 (bind-document "scratch/compteur-1" {:count 0}))

                 (atom? c1)

                 (def cat2 (catom [c1]))
                 (deref cat2)

                 (def a1 (atom 0))
                 (def a2 (atom 0))
                 (def a3 (atom 0))

                 (def cat1 (catom [a1 {:a a2 :b [42 a3]}]))

                 (swap! a1 inc)

                 (deref cat1)

                 (add-watch cat1 :w (fn [_ _ o n] (println o n)))

                 (swap! cat1 update 0 inc)

                 (swap! a1 inc))

        )



    )

(comment :maybe-useless
         (defn document-id->atom

           "usage: (document-id->atom \"scratch/counter1\")"

           [document-id]
           (-> (f/doc db document-id)
               (pull-walk)
               (atom))))




