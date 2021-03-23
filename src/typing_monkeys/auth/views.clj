(ns typing-monkeys.auth.views
  (:require [typing-monkeys.auth.module :as m]
            [typing-monkeys.utils.misc :refer [fk]]))

(m/reg-views {:login (fk [email password]
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
                                                          :on-text-changed {:event/type (m/key :type-email)}}

                                                         {:fx/type         :password-field
                                                          :v-box/margin    5
                                                          :text            password
                                                          :prompt-text     "Password"
                                                          :on-text-changed {:event/type (m/key :type-password)}}

                                                         {:fx/type      :button
                                                          :v-box/margin 5
                                                          :text         "Authenticate"
                                                          :on-action    {:event/type (m/key :authenticate)}}]}}})})