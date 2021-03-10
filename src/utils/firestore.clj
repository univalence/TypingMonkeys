(ns utils.firestore
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

(defn pull-doc [x]
  (cond
    (doc-ref? x) (f/pull-doc x)
    (map? x) x))

(defn watch-coll! [db kw on-change]
  (st/consume
   on-change
   (f/->stream (f/coll db (name kw))
               {:plain-fn vec})))

(defn overide-print-methods []

  (defmethod print-method DocumentReference [^DocumentReference dr ^Writer w]
    (.write w (str "DocumentReference:" (f/path dr))))

  (defmethod print-method DocumentSnapshot [^DocumentSnapshot ds ^Writer w]
    (.write w (str "DocumentSnapshot:" (f/path (f/ref ds)))))

  (defmethod print-method CollectionReference [^CollectionReference cr ^Writer w]
    (.write w (str "CollectionReference:" (f/path cr))))

  (defmethod print-method QuerySnapshot [^QuerySnapshot _ ^Writer w]
    (.write w "QuerySnapshot instance")))





(comment

 (overide-print-methods)
 (defonce db (f/client-with-creds
              "data/conatus-ef5f3-firebase-adminsdk-mowye-1aaf077bc3.json"))

 (type (get (.getData (.get (.get (f/doc db "rooms/room1"))))
       "members"))

 (-> (f/coll db "rooms")
     (f/filter-contains-any "members" [(f/doc db  "users/pierrebaille@gmail.com")])
     (.get)
     (.get)
     (.getDocuments))

 (-> (f/coll db "rooms")
     (f/filter-contains-any "members" [(f/doc db  "users/pierrebaille@gmail.com")])
     (f/query-snap)
     (f/query-snap->doc-snaps))

 )
