(ns chat.auth
  (:require [clj-http.client :as http]))

(do :auth

    (defn sign-in [email password ok ko]
      ;; sign in
      (http/request
       {:async        true
        :content-type :json
        :method       :post
        :url          "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
        :form-params  {:email email :password password :returnSecureToken true}}
       ok
       ko))

    #_(sign-in "pierrebaille@gmail.com" "password"
               (fn [x] (pp :ok x))
               (fn [x] (pp :ko x)))

    ;; sign up
    (defn sign-up [email password ok ko]
      (http/request
       {:async        true
        :content-type :json
        :method       :post
        :url          "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
        :form-params  {:email email :password password :returnSecureToken true}}
       ok
       ko))

    #_(sign-in "pierrebaille@gmail.com" "password"
               (fn [x] (pp :ok x))
               (fn [x] (pp :ko x)))
    )