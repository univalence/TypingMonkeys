(ns typing-monkeys.home.core
  (:require [typing-monkeys.chat.module :as chat]))

(defn init [*state]
  (chat.handler/start! *state))

(defn tab [kw content]
  {:fx/type :tab
   :text (name kw)
   :closable false
   :content content})

(def tabs
  {:chat chat/view})

(defn view [*state]
  {:fx/type :stage
   :showing true
   :title   "Typing Monkeys"
   :x 1000
   :y -500
   :scene   {:fx/type :scene
             :root    {:fx/type     :tab-pane
                       :pref-width  960
                       :pref-height 540
                       :tabs        (mapv (fn [[name view]] (tab name (view *state)))
                                          tabs)}}})

(def handler chat/handler)