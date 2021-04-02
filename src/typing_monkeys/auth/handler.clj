(ns typing-monkeys.auth.handler
  (:require [typing-monkeys.base :refer [handler *state]]
            [clj-http.client :as http]))

(defmethod handler :auth.logout
  [_]
  (swap! *state dissoc :user))

(defmethod handler :auth.type-email
  [{:keys [fx/event]}]
  (swap! *state assoc-in [:auth :email] event))

(defmethod handler :auth.type-password
  [{:keys [fx/event]}]
  (swap! *state assoc-in [:auth :password] event))

(defmethod handler :auth.authenticate
  [_]
  (let [{:keys [email password]} (get @*state :auth)]
    (http/request
     {:async        true
      :content-type :json
      :method       :post
      :url          "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
      :form-params  {:email email :password password :returnSecureToken true}}
     (fn [res] (handler {:event/type :user.signed-in :email email :response res}))
     (fn [err] (handler {:event/type :auth.sign-in-error :error err})))))
