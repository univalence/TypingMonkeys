(ns typing-monkeys.base2)

(defonce *state (atom {:auth {:email "pierrebaille@gmail.com" :password "password"}}))

(defmulti handler :event/type)