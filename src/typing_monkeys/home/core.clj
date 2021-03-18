(ns typing-monkeys.home.core
  (:require [typing-monkeys.chat.core :as chat]))

(defn tab [name content]
  {:fx/type :tab
   :text name
   :closable false
   :content content})

(def tabs
  {:chat chat/view})

(def view [*state]
  {:fx/type :stage
   :showing true
   :title   "Typing Monkeys"
   :scene   {:fx/type :scene
             :root    {:fx/type     :tab-pane
                       :pref-width  960
                       :pref-height 540
                       :tabs        (mapv (fn [[name view]] (tab name (view *state)))
                                          tabs)}}})