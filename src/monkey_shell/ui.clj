(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as ui]
            [monkey-shell.state :as shell-state]
            [clojure.walk :as walk]))

(defn members->true []
  "TODO : MOVE TO \"DATA\" NAMESPACE"
  (zipmap (get-in @shell-state/*state
                  [:shell-sessions (keyword (get-in @shell-state/*state [:session :id]))
                   :members]) (repeat true)))

(defn text-thread
  "Chronologically ordered text.
  ATM it only prints the last cmd stdout,
  TODO print full history "
  [state]
  {:fx/type      :scroll-pane
   :pref-width   400
   :pref-height  400
   :v-box/vgrow  :always
   :fit-to-width true
   :content      {:fx/type  :v-box
                  :children [{:fx/type :text
                              :text    (-> (get-in state [:shell-sessions (keyword (get-in state [:session :id])) :history])
                                           first
                                           :result
                                           :out
                                           str)}]}})

(defn shell [state] {:fx/type :stage
                     :showing true
                     :width   600
                     :height  600
                     :scene   {:fx/type :scene
                               :root    {:fx/type  :v-box
                                         :children [(ui/text-entry :capture-new-room-text :create-session "Add Session")
                                                    {:fx/type  :h-box
                                                     :children [(ui/sidebar :handle-sidebar-click
                                                                            (keys (walk/stringify-keys
                                                                                    (get state :shell-sessions))))
                                                                {:fx/type  :v-box
                                                                 :children [(text-thread state)
                                                                            (ui/text-entry :capture-text :execute)
                                                                            (ui/squared-btn
                                                                              (str (get-in @shell-state/*state [:session :id]) "'s settings")
                                                                              :open-settings)]}]}]}}})

(defn member-selection [state]
  (ui/window (:settings-window state)
             (ui/vbox [(ui/radio-group (members->true))
                       (ui/text-entry :capture-new-member-text :add-member "Add member")
                       (ui/squared-btn "OK" :close-settings)])))


(defn root [state]
  (ui/many [(shell state)
            (member-selection state)]))