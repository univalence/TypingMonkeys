(ns monkey-shell.containers.shell-opts
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.components.core :as ui]))

(def *state (atom {:user           {:id "bastien@univalence.io"}
                   :input          nil
                   :session        {:id "first-shell"}

                   :shell-sessions {:first-shell  {:history []
                                                   :members []}
                                    :second-shell {:history []
                                                   :members []}}}))

(defn create-session! [session-name]
  (swap! *state
         (fn [state]
           (assoc-in state [:shell-sessions (keyword session-name)] {:history []
                                                                     :members []}))))
(defn execute!
  "TODO DOC"
  [shell-session]
  #_(println (get-in @*state [:histories (keyword shell-session)]))
  (swap! *state
         (fn [state]
           (let [cmd-args (str/split (:input state) #" ")
                 result (apply shell/sh cmd-args)]

             (update-in state [:shell-sessions (keyword shell-session) :history] conj {:cmd-args cmd-args
                                                                                       :result   result})))))

(defn handler [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn map-event-handler [event]
  #_(println (get event :session-id))
  (case (:event/type event)
    :capture-text (swap! *state assoc :input (get event :fx/event))
    :execute (do (execute! (get-in @*state [:session :id])))
    :handle-sidebar-click (swap! *state assoc-in [:session :id] (get event :click-payload))
    :create-session (println "TODO create session")
    ))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   600
                    :height  600
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :v-box
                                        :children [(ui/squared-btn "✚" :create-session)
                                                   {:fx/type  :h-box
                                                    :children [(ui/sidebar :handle-sidebar-click
                                                                           (keys (walk/stringify-keys
                                                                                   (get state :shell-sessions))))
                                                               {:fx/type  :v-box
                                                                :children [(ui/text-thread state)
                                                                           (ui/text-entry :capture-text :execute)]}]}]}}})

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

(println @*state)