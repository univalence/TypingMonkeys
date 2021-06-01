(ns monkey-shell.core
  (:require [cljfx.api :as fx]
            [clojure.string :as str])
  (:use [clojure.java.shell :only [sh]]))

(def *state (atom {}))

(defn execute []
  (swap! *state
         (fn [state]
                  (assoc state :out (str (get (apply sh (str/split (:text state) #" ")) :out ))))
         ))

(defn handler [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn map-event-handler [event]
  (case (:event/type event)
    :capture-text (swap! *state assoc :text (get event :fx/event))
    :execute (execute)
    ))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   600
                    :height  200
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :v-box
                                        :children [{:fx/type :text
                                                    :text    (get state :out)}
                                                   {:fx/type  :h-box
                                                    :children [{:fx/type         :text-field
                                                                :on-text-changed {:event/type :capture-text}}
                                                               {:fx/type    :button
                                                                :text       "ENTER"
                                                                :pref-width 100
                                                                :on-action  {:event/type :execute}}]}]}}})


(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

