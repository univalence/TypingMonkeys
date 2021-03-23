(ns typing-monkeys.chat.module
  (:refer-clojure :exclude [get set key])
  (:require [typing-monkeys.base :refer [defmodule]]))

(defmodule :chat)

(comment

 ;; temp: DEV ------
 (swap! *state assoc :auth {:email "pierrebaille@gmail.com" :password "password"})

 (def renderer
   (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type views/root)
    :opts {:fx.opt/map-event-handler state/event-handler}))

 (fx/mount-renderer *state renderer)


 (comment :scratch

          (state/log)))