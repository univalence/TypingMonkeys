(ns monkey-shell.components.core
  (:require [cljfx.api :as fx]))

(defn many
  "Component that allow multiple window-ing"
  [component-vector]
  {:fx/type fx/ext-many
   :desc    component-vector})

#_(defn vbox
  "Vbox wrapper"
  [component-vector]
  {:fx/type :v-box
   :children (remove nil? component-vector)})

(defn vbox
  "Vbox wrapper"
  ([component-vector]
   (vbox {} component-vector))
  ([props-map component-vector]
   (merge {:fx/type  :v-box
           :children (remove nil? component-vector)}
          props-map)))

(defn hbox
  "Hbox wrapper"
  ([component-vector]
   (hbox {} component-vector))
  ([props-map component-vector]
   (merge {:fx/type  :h-box
           :children (remove nil? component-vector)}
          props-map)))

(defn squared-btn
  "squared button that return its name on click"
  [props-map click-event]
  (merge {:fx/type    :button
          :pref-width 150
          :on-action  (cond (keyword? click-event) {:event/type click-event}
                            (map? click-event) click-event)}
         props-map))

(defn text-entry
  "Text bar with enter button"
  ([on-text-change-event-keyword on-action-event-keyword]
   (text-entry on-text-change-event-keyword on-action-event-keyword "ENTER"))
  ([on-text-change-event-keyword on-action-event-keyword text]
   (hbox {} [{:fx/type         :text-field
              :on-text-changed {:event/type on-text-change-event-keyword}}
             (squared-btn {:text text} on-action-event-keyword)])))

(defn sidebar
  "Menu-like component (list of buttons)"
  [on-action-event-keyword btn-list]
  (vbox (mapv #(squared-btn {:text %} {:event/type    on-action-event-keyword
                                       :click-payload %})
              btn-list)))

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


