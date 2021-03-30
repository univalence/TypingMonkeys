(ns typing-monkeys.auth2.view)

(defn login [{:keys [email password]}]
  {:fx/type :stage
   :showing true
   :width   200
   :height  200
   :x       500 :y -1000
   :title   "Authentication"
   :scene   {:fx/type :scene
             :root    {:fx/type  :v-box
                       :padding  10
                       :children [{:fx/type         :text-field
                                   :v-box/margin    5
                                   :text            email
                                   :prompt-text     "Email"
                                   :on-text-changed {:event/type :auth.type-email}}

                                  {:fx/type         :password-field
                                   :v-box/margin    5
                                   :text            password
                                   :prompt-text     "Password"
                                   :on-text-changed {:event/type :auth.type-password}}

                                  {:fx/type      :button
                                   :v-box/margin 5
                                   :text         "Authenticate"
                                   :on-action    {:event/type :auth.authenticate}}]}}})