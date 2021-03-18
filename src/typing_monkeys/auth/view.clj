(ns typing-monkeys.auth.view)

(defn event [k]
  (keyword "typing-monkeys.auth" (name k)))

(defn login [{{:keys [email password]} :auth}]
  {:fx/type :stage
   :showing true
   :width   200
   :height  200
   :title   "Authentication"
   :scene   {:fx/type :scene
             :root    {:fx/type  :v-box
                       :padding  10
                       :children [{:fx/type         :text-field
                                   :v-box/margin    5
                                   :text            email
                                   :prompt-text     "Email"
                                   :on-text-changed {:event/type (event :type-email)}}

                                  {:fx/type         :password-field
                                   :v-box/margin    5
                                   :text            password
                                   :prompt-text     "Password"
                                   :on-text-changed {:event/type (event :type-password)}}

                                  {:fx/type      :button
                                   :v-box/margin 5
                                   :text         "Authenticate"
                                   :on-action    {:event/type (event :authenticate)}}]}}})