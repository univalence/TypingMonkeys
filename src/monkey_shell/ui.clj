(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as ui]
            [clojure.walk :as walk]))

(defn focused-session [state]
  (get-in state [:shell-sessions (:focused-session state)]))

(defn members->true [state]
  "TODO : MOVE TO \"DATA\" NAMESPACE"
  (as-> (focused-session state) _
        (get _ :members)
        (map :id _)
        (zipmap _ (repeat true))))

(defn text-thread
  "Chronologically ordered text.
  ATM it only prints the last cmd stdout,
  TODO print full history "
  [state]
  {:fx/type :scroll-pane
   :pref-width 400
   :pref-height 400
   :v-box/vgrow :always
   :fit-to-width true
   :content {:fx/type :v-box
             :children [{:fx/type :text
                         :text (-> (focused-session state)
                                   :history last :out str)}]}})

(defn session [state]
  {:fx/type :stage
   :showing true
   :width 600
   :height 600
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :on-key-pressed {:event/type :keypressed}
                  :children [(ui/text-entry :ui.session.set-new-id :new-session "Add Session")
                             {:fx/type :h-box
                              :children [(ui/sidebar :ui.sidebar.click
                                                     (keys (walk/stringify-keys
                                                             (get state :shell-sessions))))
                                         {:fx/type :v-box
                                          :children [(text-thread state)
                                                     (ui/text-entry :ui.session.set-input :execute)
                                                     (ui/squared-btn
                                                       (str (get state :focused-session) "'s settings")
                                                       :ui.session.settings.open)]}]}]}}})

(defn session-settings [state]
  (ui/window (get-in state [:ui :session :settings :window])
             (ui/vbox [(ui/radio-group (members->true state))
                       (ui/text-entry :ui.session.settings.set-new-id :add-member "Add member")
                       (ui/squared-btn "OK" :ui.session.settings.close)])))

(defn root [state]
  (ui/many [(session state)
            (session-settings state)]))