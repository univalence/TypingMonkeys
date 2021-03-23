(ns xp.cljfx.app-structure-xp)

{:context {:title "title"}

 :events {:http :http}

 :effects  {:http (fn [req dispatch] ())}

 :module  {:user  {:events  {}

                   :effects {}

                   :views   {}}

           :auth  {:events  {:sign-in :auth.sign-in
                             :sign-up :auth.sign-up} ;; simple keywords are directly mapped to matching effects

                   :effects {:sign-in (fn [{:keys [email password]} dispatch] ())
                             :sign-up (fn [{:keys [email password]} dispatch] ())}}

           :store {:events  {:connect :store.connect}

                   :effects {:connect (fn [_ dispatch])}}

           :chat  {:module {:room    {:events  {}

                                      :effects {}

                                      :views   {}}

                            :message {:events  {}

                                      :effects {}

                                      :views   {}}}}

           :text  {:events  {}

                   :effects {}

                   :views   {}}}}