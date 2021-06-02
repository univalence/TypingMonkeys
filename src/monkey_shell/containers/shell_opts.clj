(ns monkey-shell.containers.shell-opts
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.components.core :as ui]))


(def *state (atom {:history       ()
                   :sessions      (list "first-shell" "second-shell" "third-shell")
                   :shell-session {:id "first-shell"}
                   :histories     {:first-shell  ()
                                   :second-shell ()
                                   :third-shell  ()}}))

(defn execute!
  "TODO DOC"
  [shell-session]
  #_(println(get-in @*state [:histories (keyword shell-session)]))
  (swap! *state
         (fn [state]
           (let [cmd-args (str/split (:text state) #" ")
                 result (apply shell/sh cmd-args)]

             (update-in state [:histories (keyword shell-session)] conj {:cmd-args cmd-args
                                                                     :result   result})

             ))))

(defn handler [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn map-event-handler [event]
  #_(println (get event :session-id))
  (case (:event/type event)
    :capture-text (swap! *state assoc :text (get event :fx/event))
    :execute (do (execute! (get-in @*state [:shell-session :id]) ))
    :handle-sidebar-click (swap! *state assoc-in [:shell-session :id] (get event :session-id))
    ))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   600
                    :height  200
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :h-box
                                        :children [(ui/sidebar :handle-sidebar-click (get state :sessions))
                                                   {:fx/type  :v-box
                                                    :children [(ui/text-thread state)
                                                               (ui/text-entry :capture-text :execute)]}]}}})

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

(println @*state)