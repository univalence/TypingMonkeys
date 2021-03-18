(ns typing-monkeys.chat.data
  (:require [typing-monkeys.utils.misc :as u]
            [typing-monkeys.db :as db]
            [typing-monkeys.auth.data :as auth-data]
            [firestore-clj.core :as f]))

(def PATH [:chat])

(def ZERO {:room nil :input ""})

(defn path [& xs]
  (into PATH xs))

(defn message [user content]
  {:content   content
   :from      user
   :timestamp (System/nanoTime)})

(defn message-ref->data [ref]
  (let [message-ref (f/pull-doc ref)]
    (db/with-ref ref
                 {:id        (f/id ref)
                  :content   (get message-ref "content")
                  :from      (auth-data/user-ref->data (get message-ref "from"))
                  :timestamp (get message-ref "timestamp")})))

(defn room-ref->data [ref]
  (let [pulled (f/pull-doc ref)
        message-ref (f/coll ref "messages")
        members-ref (get pulled "members")]
    (db/with-ref ref
                 {:id       (f/id ref)
                  :messages (db/with-ref message-ref (mapv message-ref->data (f/docs message-ref)))
                  :members  (db/with-ref members-ref (mapv auth-data/user-ref->data members-ref))})))