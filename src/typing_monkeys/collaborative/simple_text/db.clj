(ns typing-monkeys.collaborative.simple-text.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]
            [typing-monkeys.utils.misc :as u :refer [pp]]))

(require '[xp.data-laced-with-history :as d])

(pp d/data)

(def tree (reduce d/insert d/zero d/data))

(pp tree)



(f/create! (f/doc db "trees/first")
           (u/walk_keyword->string tree))