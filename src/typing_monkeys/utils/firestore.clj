(ns typing-monkeys.utils.firestore
  (:require [firestore-clj.core :as f]
            [manifold.stream :as st])
  (:import (com.google.cloud.firestore DocumentReference CollectionReference Firestore DocumentSnapshot QuerySnapshot)
           (java.io Writer)))

(defn doc-ref? [x]
  (instance? DocumentReference x))

(defn coll-ref? [x]
  (instance? CollectionReference x))

(defn ref? [x]
  (or (doc-ref? x)
      (coll-ref? x)))

(defn overide-print-methods []

  (defmethod print-method DocumentReference [^DocumentReference dr ^Writer w]
    (.write w (str "DocumentReference:" (f/path dr))))

  (defmethod print-method DocumentSnapshot [^DocumentSnapshot ds ^Writer w]
    (.write w (str "DocumentSnapshot:" (f/path (f/ref ds)))))

  (defmethod print-method CollectionReference [^CollectionReference cr ^Writer w]
    (.write w (str "CollectionReference:" (f/path cr))))

  (defmethod print-method QuerySnapshot [^QuerySnapshot _ ^Writer w]
    (.write w "QuerySnapshot instance")))
