(ns typing-monkeys.auth.try
  (:require [typing-monkeys.base :as base]
            [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [typing-monkeys.auth.module :as auth]
            typing-monkeys.auth.events
            typing-monkeys.auth.effects
            typing-monkeys.auth.views))

(base/initialize-event-multi!)

(defn root [{:keys [fx/context]}]
  (println "rendering root")
  (if (fx/sub-val context :signed-in)
    {:fx/type :stage
     :showing true
     :width   200
     :height  200
     :x 500
     :y -1000
     :title   "Authentication"
     :scene   {:fx/type :scene
               :root    {:fx/type  :v-box
                         :padding  10
                         :children [{:fx/type :label
                                     :text "Welcome"}]}}}
    {:fx/type (get-in @base/*app [:views :auth.login])}))

(defonce app
  (fx/create-app base/*context
                 :event-handler base/event-handler
                 :effects (:effects @base/*app)
                 :co-effects (:co-effects @base/*app {})
                 :desc-fn (fn [_] {:fx/type root})))

((:renderer app))