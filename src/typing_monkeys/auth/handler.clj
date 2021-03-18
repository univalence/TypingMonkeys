(ns typing-monkeys.auth.handler
  (:require [typing-monkeys.auth.api :as api]
            [typing-monkeys.auth.db :as db]
            [typing-monkeys.auth.data :as data]))

(defn auth-event
  "check if an event is an auth event, returns its simple name as a keyword if so"
  [{:as event
    t   :event/type}]
  (when (= "typing_monkeys.auth" (namespace t))
    (keyword (name t))))

(defn event-handler
  "build an event handler for the auth component from the given *state (atom)"
  [*state]
  (fn [event]
    (when-let [event-name (auth-event event)]
      (case event-name
        :type-password (swap! *state assoc-in (data/path :password) (:fx/event event))
        :type-email (swap! *state assoc-in (data/path :email) (:fx/event event))
        :logout (swap! *state assoc-in data/PATH data/ZERO)
        :authenticate (let [{:keys [email password]} (get-in @*state data/PATH)]
                        (db/sign-in email password
                                    (fn [_] (swap! *state assoc (db/get-user email)))
                                    (fn [e] (println "cannot login " e))))))))