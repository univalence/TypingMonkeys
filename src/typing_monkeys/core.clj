(ns typing-monkeys.core
  (:require [typing-monkeys.auth.core :as auth]
            [typing-monkeys.home.core :as home]
            [cljfx.api :as fx]))

(def *state (atom {}))

(def handler
  (juxt auth/handler
        home/handler))

(defn root [{:as state :keys [user]}]
  (if user
    (home/view state)
    (auth/view state)))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
   :opts {:fx.opt/map-event-handler handler}))

(fx/mount-renderer *state renderer)