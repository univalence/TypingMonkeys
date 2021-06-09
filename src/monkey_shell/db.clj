(ns monkey-shell.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]
            [clojure.walk :as walk])
  (:import (java.util HashMap ArrayList)))

(defonce db
  (f/client-with-creds "data/unsafe.json"))

(defn fetch-user-sessions
  [user-id]
  (-> (f/coll db "shell-sessions")
      (f/filter-contains "members" (f/doc db (str "users/" user-id)))))

(defn pull-walk [x]
  (cond
    (or (fu/ref? x) (fu/query? x)) (fu/with-ref x (pull-walk (f/pull x)))
    (map? x) (into {} (map (fn [[k v]] [(keyword k) (pull-walk v)]) x))
    (vector? x) (mapv pull-walk x)
    (instance? HashMap x) (pull-walk (into {} x))
    (instance? ArrayList x) (pull-walk (into [] x))
    :else x))

(defn fetch-user
  [user-id]
  (-> (f/doc db (str "users/" user-id))
      (pull-walk)
      (assoc :id user-id)))

(defn watch! [x callback]
  (st/consume (fn [x]
                #_(println "consume " x)
                (let [data (pull-walk x)]
                  (callback data)))
              (f/->stream x)))

(defn sync-session! [session]
  (-> (f/coll db "shell-sessions")
      (f/doc (:id session))
      (f/set! (-> session
                  (update :members (partial mapv fu/data->ref))
                  (update :host fu/data->ref)
                  (dissoc :id)
                  (walk/stringify-keys)))))
