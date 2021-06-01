(ns typing-monkeys.user.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.utils.walk :as walk]))

(defn user-ref->data [ref]
  (let [user (f/pull-doc ref)]
    (db/with-ref ref
                 {:id          (f/id ref)
                  :pseudo      (get user "pseudo")
                  :color       (get user "color")
                  :description (get user "description")})))

(defn get-user [email]
  (-> (f/coll db "users")
      (f/doc email)
      user-ref->data))

(defn set-user! [data]
  (println "set user " data)
  (f/set! (db/data->ref data)
          (walk/stringify-keys data)))

(defn get-user-ids []
  (mapv f/id (f/docs (f/coll db "users"))))