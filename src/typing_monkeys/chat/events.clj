(ns typing-monkeys.chat.events
  (:require [cljfx.api :as fx]
            [cheshire.core :as cheschire]
            [typing-monkeys
             [base :as b]
             [chat.module :as m]
             [utils.misc :refer [fk pp]]]))

(m/reg-events {:init          (fk [fx/context]
                                  {:fx/context (m/set context :email nil :password nil)})

               })