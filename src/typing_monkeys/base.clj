(ns typing-monkeys.base
  (:import (java.util UUID)))

(def client-id
  (.toString (UUID/randomUUID)))

(def initial-state
  {:module :text
   :auth {:email "pierrebaille@gmail.com"
          :password "password"}})

(defonce *state
  (atom initial-state))

(defn reset-state! []
  (reset! *state initial-state))

(defmulti handler :event/type)