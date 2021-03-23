(ns typing-monkeys.text.views
  (:require [cljfx.api :as fx]
            [xp.data-laced-with-history :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [clojure.string :as string]))

(defn flow-pane [xs]
  {:fx/type  :flow-pane
   :vgap     5
   :hgap     5
   :padding  20
   :children xs})

(defn stage [scene]
  {:fx/type :stage
   :showing true
   :width   800
   :height  300
   :x       500
   :y       -1000
   :scene   (assoc scene :fx/type :scene)})

(defn user-ref->str [user-ref]
  (->> (string/split (str user-ref) #"/")
       last butlast (apply str)))

(defn member [{:keys [color user]}]
  {:fx/type  :h-box
   :children [{:fx/type :circle :center-x 0 :center-y 0 :radius 7 :style {:-fx-stroke color :-fx-fill color}}
              {:fx/type :label
               :text    (user-ref->str user)
               :style   {:-fx-padding [0 10 0 5]}}]})

(defn root
  [{:keys [tree position color members member-id user]}]
  (let [tree-seq (cons [[0 0] "" true] (d/tree-seq tree))
        position->color
        (fn [p]
          (or (and (= position p) color)
              (some (fn [{:keys [position color]}]
                      (when (= position p) color)) members)))]

    (stage {:on-key-released {:event/type :text.keypressed}
            :root            {:fx/type  :v-box
                              :children [(flow-pane (mapv member (cons {:id member-id :position position :color color :user user} members)))
                                         (flow-pane (keep (fn [[position char visible]]
                                                            (when visible
                                                              {:fx/type :label :text char
                                                               :style   {:-fx-font-family  "monospace"
                                                                         :-fx-font-size    20
                                                                         :-fx-alignment    :center
                                                                         :-fx-border-width [0 3 0 0]
                                                                         :-fx-border-color (or (position->color position) "transparent")}}))
                                                          tree-seq))]}})))