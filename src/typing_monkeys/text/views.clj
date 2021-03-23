(ns typing-monkeys.text.views
  (:require [cljfx.api :as fx]
            [xp.data-laced-with-history :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [clojure.string :as string]))

(defn contributor [{:keys [color user]}]
  {:fx/type  :h-box
   :children [{:fx/type :circle :center-x 0 :center-y 0 :radius 7 :style {:-fx-stroke color :-fx-fill color}}
              {:fx/type :label
               :text    (apply str (butlast (last (string/split (str user) #"/"))))
               :style   {:-fx-padding [0 10 0 5]}}]})

(defn root
  [{:keys [tree position color members site-id]}]
  (println "meta tree" (meta tree) members)
  (let [tree-seq (cons [[0 0] "" true] (d/tree-seq tree))
        position->color (fn [p] (if (= position p) color
                                                   (or (first (keep (fn [{:keys [position color]}] (when (= position p) color))
                                                                    (remove (fn [{:keys [id]}] (= site-id id)) members)))
                                                       "transparent")))]
    #_(println color)
    {:fx/type :stage
     :showing true
     :width   800
     :height  300
     :x       500
     :y       -1000
     :scene   {:fx/type         :scene
               ;;:stylesheets [ss/stylesheet]
               :on-key-released {:event/type :text.keypressed}
               :root            {:fx/type  :v-box
                                 :children [{:fx/type  :flow-pane
                                             :vgap     5
                                             :hgap     5
                                             :padding  20
                                             :children (cons {:fx/type :label :text "contributors: "}
                                                             (mapv contributor members))}
                                            {:fx/type  :flow-pane
                                             :vgap     5
                                             :hgap     5
                                             :padding  20
                                             :children (keep (fn [[position char visible]]
                                                               (when visible
                                                                 {:fx/type :label :text char
                                                                  :style   {:-fx-font-family  "monospace"
                                                                            :-fx-font-size    20
                                                                            :-fx-alignment    :center
                                                                            :-fx-border-width [0 3 0 0]
                                                                            :-fx-border-color (position->color position)}}))
                                                             tree-seq)}]}}}))