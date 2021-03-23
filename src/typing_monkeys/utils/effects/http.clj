(ns typing-monkeys.utils.effects.http
  (:require [clj-http.client :as http]))

(defn http-effect [v dispatch!]
  (try
    (http/request
     (-> v
         (assoc :async? true :as :byte-array)
         (dissoc :on-response :on-exception))
     (fn [response]
       (dispatch! (assoc (:on-response v) :response response)))
     (fn [exception]
       (dispatch! (assoc (:on-exception v) :exception exception))))
    (catch Exception e
      (dispatch! (assoc (:on-exception v) :exception e)))))