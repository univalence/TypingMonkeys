(ns monkey-shell.containers.shell-opts
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.components.core :as ui]))


(def *state (atom {:history       ()
                   :shell-session {:id "first-shell"}}))

(defn execute! []
  (swap! *state
         (fn [state]
           (let [cmd-args (str/split (:text state) #" ")
                 result (apply shell/sh cmd-args)]
             (update state :history conj {:cmd-args cmd-args
                                          :result   result})))))

(defn handler [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn map-event-handler [event]
  (case (:event/type event)
    :capture-text (swap! *state assoc :text (get event :fx/event))
    :execute (do (execute!))
    :handle-sidebar-click (println "test")
    ))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   600
                    :height  200
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :h-box
                                        :children [(ui/sidebar :handle-sidebar-click)
                                                   {:fx/type  :v-box
                                                    :children [(ui/text-thread state)
                                                               (ui/text-entry :capture-text :execute)]}]}}})

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

(println (get @*state :history))