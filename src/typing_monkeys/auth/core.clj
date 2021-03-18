(ns typing-monkeys.auth.core
  (:require [typing-monkeys.auth.view :as v]
            [typing-monkeys.auth.handler :as h]
            [typing-monkeys.auth.data :as data]))

(def handler h/event-handler)

(def view v/login)

(def initial-data data/ZERO)
