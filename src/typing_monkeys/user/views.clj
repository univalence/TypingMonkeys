(ns typing-monkeys.user.views
  (:require [cljfx.api :as fx]
            [typing-monkeys.utils.cljfx :refer [defc]]))

(defn label [text]
  {:fx/type   :h-box
   :alignment :top-right
   :pref-width 120
   :children  [{:fx/type :label
                :text text}]})

(defc v-box [& children]
      :padding 5
      :spacing 5)

(defc h-box [& children]
      :padding 5
      :spacing 5)

(defn editor

  [{:keys [pseudo color description]}]

  {:center (v-box

            (h-box (label "pseudo: ")
                   {:fx/type         :text-field
                    :text            pseudo
                    :on-text-changed {:event/type :user.assoc :key :pseudo}})

            (h-box (label "color: ")
                   {:fx/type          :color-picker
                    :value            (or color :grey)
                    :on-value-changed {:event/type :user.assoc :key :color}})

            (h-box (label "description: ")
                   {:fx/type         :text-area
                    :text            description
                    :on-text-changed {:event/type :user.assoc :keys :description}}))})

