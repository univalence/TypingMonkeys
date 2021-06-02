(ns monkey-shell.components.core)

(defn text-entry
  "Text bar with enter button"
  [on-text-change-event-keyword on-action-event-keyword]
  {:fx/type  :h-box
   :children [{:fx/type         :text-field
               :on-text-changed {:event/type on-text-change-event-keyword}}
              {:fx/type    :button
               :text       "ENTER"
               :pref-width 100
               :on-action  {:event/type on-action-event-keyword}}]})

(defn text-thread
  "Chronologically ordered text.
  ATM it only prints the last cmd stdout,
  TODO print full history "
  [state]
  {:fx/type      :scroll-pane
   :v-box/vgrow  :always
   :fit-to-width true
   :content      {:fx/type  :v-box
                  :children [{:fx/type :text
                              :text    (-> (get state :history)
                                           first
                                           :result
                                           :out
                                           str)}]}})

(defn sidebar
  "Menu-like component (list of buttons)"
  [on-action-event-keyword]
  {:fx/type  :v-box
   :children [{:fx/type    :button
               :text       "btn"
               :pref-width 150
               :on-action  {:event/type on-action-event-keyword}}
              {:fx/type    :button
               :text       "btn2"
               :pref-width 150
               :on-action  {:event/type on-action-event-keyword}}
              ]})
