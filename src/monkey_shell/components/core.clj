(ns monkey-shell.components.core)

(defn vbox
  "Vbox wrapper"
  [component-vector]
  {:fx/type  :v-box
   :padding  20
   :spacing  10
   :children component-vector})

(defn hbox
  "Hbox wrapper"
  [component-vector]
  {:fx/type  :h-box
   :padding  20
   :spacing  10
   :children component-vector})

(defn squared-btn
  "squared button that return its name on click"
  [text on-action-event-keyword]
  {:fx/type    :button
   :text       text
   :pref-width 150
   :on-action  {:event/type    on-action-event-keyword
                :click-payload text}})

(defn text-entry
  "Text bar with enter button"
  ([on-text-change-event-keyword on-action-event-keyword]
   (text-entry on-text-change-event-keyword on-action-event-keyword "ENTER"))
  ([on-text-change-event-keyword on-action-event-keyword text]
   (hbox [{:fx/type         :text-field
           :on-text-changed {:event/type on-text-change-event-keyword}}
          (squared-btn text on-action-event-keyword)])))

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

(defn sidebar
  "Menu-like component (list of buttons)"
  [on-action-event-keyword btn-list]
  (vbox (mapv #(squared-btn % on-action-event-keyword) btn-list)))

(defn window
  "Window component"
  [option-map root-component]
  (merge {:fx/type       :stage
          :always-on-top true
          :showing       true
          :scene         {:fx/type :scene
                          :root    root-component}}
         option-map))

(defn radio-btn
  "Single radio button,
  can be used in radio group"
  [selected? on-action-event-keyword text]
  {:fx/type  :radio-button
   :selected selected?
   :text     text})

(defn radio-group
  "Vertically organized radio buttons"
  [id->true?]
  (vbox
    (mapv #(radio-btn (last %) :fixme (first %)) id->true?)))
