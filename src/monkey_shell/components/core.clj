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
   :pref-width 400
   :pref-height 400
   :v-box/vgrow  :always
   :fit-to-width true
   :content      {:fx/type  :v-box
                  :children [{:fx/type :text
                              :text    (-> (get-in state [:shell-sessions (keyword (get-in state [:session :id])) :history  ])
                                           first
                                           :result
                                           :out
                                           str)}]}})

(defn squared-btn
  "squared button that return its name on click"
  [text on-action-event-keyword]
  {:fx/type  :h-box
   :spacing  5
   :padding  5
   :children [{:fx/type    :button
               :text       text
               :pref-width 150
               :on-action  {:event/type on-action-event-keyword
                            :click-payload text}}]})

(defn sidebar
  "Menu-like component (list of buttons)"
  [on-action-event-keyword btn-list]
  {:fx/type  :v-box
   :children (mapv #(squared-btn % on-action-event-keyword) btn-list)})

