(ns xp.cljfx.scratch
  (:require [cljfx.api :as fx]))

(fx/on-fx-thread
 (fx/create-component
  {:fx/type :stage
   :showing true
   :always-on-top true
   :style :transparent
   :scene {:fx/type :scene
           :fill :transparent
           :stylesheets #{"styles.css"}
           :root {:fx/type :v-box
                  :children [{:fx/type :label
                              :pref-width 200
                              :style {:-fx-text-alignment :right}
                              :tooltip {:fx/type :tooltip
                                        :text "I am a tooltip!"}
                              :text "Hi! What's your name?"}
                             {:fx/type :text-field}]}}}))