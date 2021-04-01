(ns typing-monkeys.base)

(def initial-state
  {:module :chat
   :auth {:email "pierrebaille@gmail.com"
          :password "password"}})

(defonce *state
  (atom initial-state))

(defn reset-state! []
  (reset! *state initial-state))

(defmulti handler :event/type)