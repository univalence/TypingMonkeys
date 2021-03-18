(ns typing-monkeys.text.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.utils.walk :as walk]))

(defn user_ref->data [ref]
  (let [user-ref (f/pull-doc ref)]
    (db/with-ref ref
                 {:id     (f/id ref)
                  :pseudo (get user-ref "pseudo")})))

#_(instance? java.util.ArrayList x)

(defn tree_format-data
  [data]
  (->> (into {} data) ;; sometimes data is an java.util.HashMap
       (walk/keywordize-keys)
       (walk/prewalk-replace
        (fn [x]
          ;; operations verbs will be turn into a keyword
          (when-let [[verb & args] (and (map-entry? x) (= :op (key x)) (val x))]
            [:op (vec (cons (keyword verb) args))])))))

(defn tree_ref->data [ref]
  (let [tree (f/pull-doc ref)]
    (db/with-ref ref
                 {:id      (f/id ref)
                  :data    (tree_format-data (get tree "data"))
                  :members (mapv user_ref->data (get tree "members"))})))

(defn get-first-tree []
  (tree_ref->data (f/doc db "crdt-strings/first")))






(comment

 (tree_ref->data (f/doc db "crdt-strings/first"))

 (type (get-in (tree_ref->data (f/doc db "crdt-strings/first"))
               [:data :children]))

 (walk/keywordize-keys {"a" [{"b" :c}] "b" {"c" {"d" :m}}})

 (walk/keywordize-keys (get (f/pull-doc (f/doc db "crdt-strings/first")) "data")))

(comment :first-tree-init
         (require '[xp.data-laced-with-history :as d])
         (f/create! (f/doc db "crdt-strings/first")
                    {"members" [(f/doc db "users/pierrebaille@gmail.com")
                                (f/doc db "users/francois@univalence.io")]
                     "colors"  {"pierrebaille@gmail.com" "lightskyblue"
                                "francois@univalence.io" "tomato"}
                     "data"    (u/walk_keyword->string (reduce d/insert d/zero d/data))}))