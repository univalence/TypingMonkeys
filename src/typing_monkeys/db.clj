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

;; user -------------------------------------------------

(defn user-ref->data [ref]
  (let [user-ref (f/pull-doc ref)]
    (with-ref ref
              {:id     (f/id ref)
               :pseudo (get user-ref "pseudo")})))

(defn get-user [email]
  (-> (f/coll db "users")
      (f/doc email)
      user-ref->data))