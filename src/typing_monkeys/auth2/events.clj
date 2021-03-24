(ns typing-monkeys.auth2.events)

{:init          (fk [fx/context]
                    {:fx/context (m/set context :email nil :password nil)})

 :logout        (fk [fx/context]
                    {:fx/context (b/set context :user nil)
                     :dispatch   {:event/type (m/key :init)}})

 :type-email    (fk [fx/event fx/context]
                    {:fx/context (m/set context :email event)})

 :type-password (fk [fx/event fx/context]
                    {:fx/context (m/set context :password event)})

 :authenticate  (fk [fx/context]
                    (let [{:keys [email password]} (m/sub context)]
                      (println "authenticate " (m/sub context) (m/key :sign-in))
                      {(m/key :sign-in) [email password]}))

 :signed-in     (fk [fx/context fx/event response]
                    (do (pp (cheschire/parse-string (:body response)))
                        {:fx/context (b/set context :signed-in true)}))

 :sign-in-error (fk [] ())}

{:sign-in (fn [[email password] dispatch]
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
             (fn [err] (dispatch (m/event :sign-up-error {:error err})))))}