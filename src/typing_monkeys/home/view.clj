(ns typing-monkeys.selector.view)

(defn button [{:keys [text on-action]}]
  {:fx/type  :h-box
   :spacing  5
   :padding  5
   :children [{:fx/type    :button
               :text       text
               :pref-width 300
               :on-action  on-action}]})

(defn view [items]
  {:fx/type :stage
   :width   300
   :height  300
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type  :grid-pane
                       :padding  10
                       :hgap     10
                       :children [{:fx/type      :scroll-pane
                                   :fit-to-width true
                                   :content      {:fx/type  :v-box
                                                  :children (mapv button items)}}]}}})

#_(selector [{:text "chat" :on-action {:event/type :typing-monkeys/select-app :id :chat}}
           {:text "text" :on-action {:event/type :typing-monkeys/select-app :id :text}}])