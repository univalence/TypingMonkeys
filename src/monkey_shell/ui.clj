(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as ui]
            [monkey-shell.state :as shell-state]
            [clojure.walk :as walk]))

(defn members->true []
  "TODO : MOVE TO \"DATA\" NAMESPACE"
  (zipmap (get-in @shell-state/*state
                  [:shell-sessions (keyword (get-in @shell-state/*state [:session :id]))
                   :members]) (repeat true)))

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
                                                                 :children [(ui/text-thread state)
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