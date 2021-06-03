(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as ui]
            [clojure.walk :as walk]))

(defn members->true [state]
  "TODO : MOVE TO \"DATA\" NAMESPACE"
  (zipmap (get-in state
                  [:shell-sessions (keyword (get-in state [:session :id]))
                   :members]) (repeat true)))

(defn shell [state]
  {:fx/type :stage
   :showing true
   :width 600
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [(ui/text-entry :ui.session.set-new-id :new-session "Add Session")
                             {:fx/type :h-box
                              :children [(ui/sidebar :ui.sidebar.click
                                                     (keys (walk/stringify-keys
                                                             (get state :shell-sessions))))
                                         {:fx/type :v-box
                                          :children [(ui/text-thread state)
                                                     (ui/text-entry :ui.session.set-input :execute)
                                                     (ui/squared-btn
                                                       (str (get-in state [:session :id]) "'s settings")
                                                       :ui.session.settings.open)]}]}]}}})

(defn member-selection [state]
  (ui/window (get-in state [:ui.session.settings.window])
             (ui/vbox [(ui/radio-group (members->true state))
                       (ui/text-entry :ui.session.settings.set-new-id :add-member "Add member")
                       (ui/squared-btn "OK" :ui.session.settings.close)])))


(defn root [state]
  (ui/many [(shell state)
            (member-selection state)]))