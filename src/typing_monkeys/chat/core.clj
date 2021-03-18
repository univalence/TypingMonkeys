(ns typing-monkeys.chat.core
  (:require [cljfx.api :as fx]
            [typing-monkeys.chat
             [view :as views]
             [data :as state :refer [*state]]]))

;; temp: DEV ------
(swap! *state assoc :auth {:email "pierrebaille@gmail.com" :password "password"})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type views/root)
   :opts {:fx.opt/map-event-handler state/event-handler}))

(fx/mount-renderer *state renderer)


(comment :scratch

         (state/log))