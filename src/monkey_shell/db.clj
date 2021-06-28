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

#_(defn user-ids [])

(defn pull-walk [x]
  (cond
    (fu/ref? x)
    (fu/with-ref x (assoc (pull-walk (f/pull x)) :db/id (f/id x)))
    (fu/query? x)
    (fu/with-ref x (pull-walk (f/pull x)))
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
                (let [data (pull-walk x)]
                  (callback data)))
              (f/->stream x)))

(defn sync-session! [session]
  (println "sync session! " (-> session :history last :out))
  (-> (f/coll db "shell-sessions")
      (f/doc (name (:id session)))
      (f/set! (-> session
                  (update :members (partial mapv fu/data->ref))
                  (update :host fu/data->ref)
                  (dissoc :id)
                  (walk/stringify-keys)))))

(defn watch-sessions! [user-id callback]
  (doseq [id (mapv first (f/pull (fetch-user-sessions user-id)))]
    (st/consume (fn [session]
                  (println "session change " id)
                  (callback id (pull-walk session)))
                (f/->stream (f/doc db (str "shell-sessions/" id))))))

(comment
  (watch-sessions! "pierrebaille@gmail.com" identity))