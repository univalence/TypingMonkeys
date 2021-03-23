(ns typing-monkeys.auth.events
  (:require [cljfx.api :as fx]
            [typing-monkeys.base :as b]
            [typing-monkeys.auth.module :as m]
            [typing-monkeys.utils.misc :refer [fk pp]]))

(m/reg-events {:init          (fk [fx/context]
                                  #_(println "init")
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
                                  {:fx/context (pp response (b/set context :signed-in true))})

               :sign-in-error (fk [] ())})


