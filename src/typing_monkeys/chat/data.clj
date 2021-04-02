(ns typing-monkeys.chat.data
  (:require [typing-monkeys.db :as db]
            [typing-monkeys.user.db :as user]
            [firestore-clj.core :as f]))

(defn message-ref->data [ref]
  (let [message (f/pull-doc ref)]
    (db/with-ref ref
                 {:id        (f/id ref)
                  :content   (get message "content")
                  :from      (user/user-ref->data (get message "from"))
                  :timestamp (get message "timestamp")})))

(defn room-ref->data [ref]
  (let [pulled (f/pull-doc ref)
        message-ref (f/coll ref "messages")
        members-ref (into [] (get pulled "members"))]
    (db/with-ref ref
                 {:id       (f/id ref)
                  :messages (db/with-ref message-ref (mapv message-ref->data (f/docs message-ref)))
                  :members  (mapv user/user-ref->data members-ref)})))