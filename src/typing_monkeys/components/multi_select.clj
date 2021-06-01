(ns typing-monkeys.components.multi-select
  (:require [typing-monkeys.utils.cljfx :refer [defc]]
            [cljfx.api :as fx]))

(def *state (atom {:ms1 {:children [{:name "one"} {:name "two" :selected true} {:name "three"}]}}))

(defn closable-label [text on-close]
  {:fx/type  :h-box
   :style    {:-fx-background-color :tomato
              :-fx-padding          4}
   :children [{:fx/type :label
               :style   {:-fx-text-fill   :white
                         :-fx-font-weight :bold}
               :text    text}
              {:fx/type          :label
               :style   {:-fx-text-fill   :white
                         :-fx-font-weight :bold}
               :text             " x "
               :on-mouse-clicked on-close}]})

(comment :first-try
         (defn multi-select [{:keys [get-state set-state]}]
           (let [{:as state :keys [children]} (get-state)
                 selected-items (filter :selected children)
                 selectable-items (remove :selected children)
                 children-by-name (reduce (fn [a [{:as i :keys [name]} idx]] (assoc a name (assoc i :index idx)))
                                          {} (map vector children (range)))]
             {:fx/type  :v-box
              :children [{:fx/type  :flow-pane
                          :children (mapv (fn [item idx]
                                            (closable-label (:name item)
                                                            (fn [_] (set-state (assoc-in state [:children idx :selected] false)))))
                                          selected-items
                                          (range))}
                         {:fx/type          :choice-box
                          :value            "select"
                          :items            (map :name selectable-items)
                          :on-value-changed (fn [e]
                                              (let [child (get children-by-name e)]
                                                (set-state (assoc-in state [:children (:index child) :selected] true))))}]})))

(defn multi-select [{:keys [get-state set-state]}]
  (let [{:as state :keys [children open? text]} (get-state)
        close! (fn [_] (set-state (assoc state :open? false)))
        children (mapv (fn [c i] (assoc c :index i)) children (range))
        selected-items (filter :selected children)
        selectable-items (remove :selected children)
        selection-toggler (fn [idx] (fn [e] (set-state (update-in state [:children idx :selected] not))))]
    {:fx/type  :v-box
     :children [{:fx/type  :v-box
                 :spacing  5
                 :padding  5
                 :style    {:-fx-text-fill :white :-fx-font-weight :bold}
                 :children (conj (mapv (fn [item idx]
                                         (closable-label (:name item)
                                                         (fn [_] (set-state (assoc-in state [:children idx :selected] false)))))
                                       selected-items
                                       (range))
                                 (if open?
                                   {:fx/type      :scroll-pane
                                    :fit-to-width true
                                    :content      {:fx/type         :v-box
                                                   :on-mouse-exited close!
                                                   :children        (mapv (fn [{:keys [name index]}]
                                                                            {:fx/type :label :text name :on-mouse-clicked (selection-toggler index)})
                                                                          selectable-items)}}
                                   {:fx/type   :button
                                    :text      "Add"
                                    :on-action (fn [_] (set-state (assoc state :open? true)))}))}
                ]}))

(defn demo []

  (defn root [state]
    {:fx/type :stage
     :showing true
     :width   500 :height 500 :x 1000 :y -1000
     :scene   {:fx/type :scene
               :root    {:fx/type  :v-box
                         :children [{:fx/type   multi-select
                                     :get-state (fn [] (:ms1 @*state))
                                     :set-state (fn [state] (swap! *state assoc :ms1 state))}]}}})

  (def renderer
    (fx/create-renderer
     :middleware (fx/wrap-map-desc assoc :fx/type root)))

  (def mounted-render (fx/mount-renderer *state renderer)))

(demo)

