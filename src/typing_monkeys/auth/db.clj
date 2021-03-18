(ns typing-monkeys.auth.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.auth.data :as data]
            [clj-http.client :as http]))

(defn get-user [email]
  (-> (f/coll db "users")
      (f/doc email)
      data/user-ref->data))

(defn sign-in [email password ok ko]
  (http/request
   {:async        true
    :content-type :json
    :method       :post
    :url          "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
    :form-params  {:email email :password password :returnSecureToken true}}
   ok
   ko))

(defn sign-up [email password ok ko]
  (http/request
   {:async        true
    :content-type :json
    :method       :post
    :url          "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
    :form-params  {:email email :password password :returnSecureToken true}}
   ok
   ko))

(comment :scratch
         (sign-in "pierrebaille@gmail.com" "password"
                  (fn [x] (println :ok x))
                  (fn [x] (println :ko x))))