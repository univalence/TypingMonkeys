(ns typing-monkeys.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]))

(fu/overide-print-methods)

(defonce db (f/client-with-creds
             "data/unsafe.json"))

(defn with-ref [ref data]
  (vary-meta data assoc :ref ref))

(defn data->ref [x]
  (-> x meta :ref))
