(ns typing-monkeys.auth.effects
  (:require [clj-http.client :as http]
            [typing-monkeys.auth.module :as m]))

(m/reg-effects {:sign-in (fn [[email password] dispatch]
                           (println "sign-in " email password)
                           (http/request
                            {:async        true
                             :content-type :json
                             :method       :post
                             :url          "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
                             :form-params  {:email email :password password :returnSecureToken true}}
                            (fn [res] (dispatch (m/event :signed-in {:response res})))
                            (fn [err] (dispatch (m/event :sign-in-error {:error err})))))

                :sign-up (fn [[email password] dispatch]
                           (http/request
                            {:async        true
                             :content-type :json
                             :method       :post
                             :url          "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
                             :form-params  {:email email :password password :returnSecureToken true}}
                            (fn [res] (dispatch (m/event :signed-up {:response res})))
                            (fn [err] (dispatch (m/event :sign-up-error {:error err})))))})

(comment :scratch
         (sign-in "pierrebaille@gmail.com" "password"
                  (fn [x] (println :ok x))
                  (fn [x] (println :ko x))))