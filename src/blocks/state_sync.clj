
(ns blocks.state-sync
  (:refer-clojure :exclude [swap!])
  (:require [clojure.core :as core]
            [monkey-shell.components.core :as comps]
            [typing-monkeys.utils.misc :as utils]
            [firestore-clj.core :as f]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st])
  (:import (java.util HashMap ArrayList)))

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
  (let [ref (fu/data->ref @atom)
        new-state (apply core/swap! atom f args)]
    (f/set! ref (clojure.walk/stringify-keys new-state))))

(defn initialize-document

  "create a firestore document with the given id and data
   returns an atom and setup the synchronisation between it and firestore"

  [id data]

  (let [*m (atom data)
        ref (f/doc db id)]

    (f/set! ref (clojure.walk/stringify-keys data))

    (watch! ref
            (fn [x]
              (when (not= @*m x)
                (core/reset! *m x))))
    *m))

(comment :exemple

    (def my-data
      (initialize-document
        "scratch/my-data"
        {:name "papa" :destination "maman" :score 0}))

    (swap! my-data update :score inc)

    (assert (= 1 (:score @my-data))))


; ---- utils ----

(defn document-id->atom
  "usage: (document-id->atom \"scratch/counter1\")"
  [document-id]
  (-> (f/doc db document-id)
      (pull-walk)
      (atom)))



