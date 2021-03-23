(ns typing-monkeys.core
  (:require [typing-monkeys.auth.core :as auth]
            [typing-monkeys.home.core :as home]
            [cljfx.api :as fx]
            [typing-monkeys.utils.misc :as u]))

(def *state (atom {:auth {:email "pierrebaille@gmail.com" :password "password"}}))

(def handler
  (juxt (auth/handler *state)
        (home/handler *state)
        (fn [event]
          (case (:event/type event)
            :typing-monkeys/logged-in (home/init *state)))))

(defn root [{:as state :keys [user]}]
  (println "rendering root" (auth/view state))
  (if user
    (home/view state)
    (auth/view state)))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
   :opts {:fx.opt/map-event-handler handler}))

(def mounted-render (fx/mount-renderer *state renderer))

#_(renderer)

(comment
 (u/pp @*state))