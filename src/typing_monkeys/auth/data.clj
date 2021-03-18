(ns typing-monkeys.auth.data
  (:require [typing-monkeys.db :as db]
            [firestore-clj.core :as f]))

(def PATH [:auth])

(def ZERO {:email nil :password nil})

(defn path [& xs]
  (into PATH xs))

(defn user-ref->data [ref]
  (let [user-ref (f/pull-doc ref)]
    (db/with-ref ref
                 {:id     (f/id ref)
                  :pseudo (get user-ref "pseudo")})))