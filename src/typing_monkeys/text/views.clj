(ns typing-monkeys.text.views
  (:require [cljfx.api :as fx]
            [xp.data-laced-with-history :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            ))

(defn root
  [{:keys [tree position]}]
  (let [tree-seq (d/tree-seq tree)]
    {:fx/type :stage
     :showing true
     :width   800
     :height  300
     :scene   {:fx/type        :scene
               ;;:stylesheets [ss/stylesheet]
               :on-key-pressed {:event/type ::press}
               :root           {:fx/type  :flow-pane
                                :vgap 5
                                :hgap 5
                                :padding  20
                                :children (keep (fn [[id char visible]]
                                                  (when visible
                                                    {:fx/type :label :text char
                                                     :style   {:-fx-font-family  "monospace"
                                                               :-fx-font-size    20
                                                               :-fx-alignment    :center
                                                               :-fx-border-width [0 3 0 0]
                                                               :-fx-border-color (if (= position id) "lightskyblue" "transparent" )}}))
                                                tree-seq)}}}))