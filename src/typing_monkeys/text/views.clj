(ns typing-monkeys.text.views
  (:require [cljfx.api :as fx]
            [xp.data-laced-with-history :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.utils.cljfx :refer [defc styled styles]]
            [clojure.string :as string]))

(defc label [text])
(defc circle [radius])

(defn flow-pane [xs]
  {:fx/type  :flow-pane
   :vgap     5
   :hgap     5
   :padding  20
   :children xs})

(defn user-ref->str [user-ref]
  (->> (string/split (str user-ref) #"/")
       last butlast (apply str)))

(defn member [{:keys [color user]}]
  {:fx/type  :h-box
   :children [(-> (circle 7)
                  (styled :stroke color :fill color))
              (-> (label (user-ref->str user))
                  (styled :padding [0 10 0 5]))]})

(defn cell [[_ char visible color]]
  (when visible
    (-> (label char)
        (styled
         :font-family "monospace"
         :font-size 20
         :alignment :center
         :border-width [0 3 0 0]
         :border-color (or color "transparent")))))

(defn root
  [{:keys [tree position color members member-id user text-ids]}]

  (let [members (cons {:id member-id :position position :color color :user user} members)
        position->color (into {} (map (juxt :position :color) (reverse members)))
        cells (cons [[0 0] "" true] (d/tree-seq tree))
        visible-cells (filter (fn [c] (nth c 2)) cells)
        colored-cells (mapv (fn [c] (conj c (position->color (first c)))) visible-cells)]

    {:left   {:fx/type       :scroll-pane
              :content       {:fx/type  :v-box
                              :padding 5
                              :spacing 5
                              :children (mapv (fn [x] {:fx/type :button :text x :pref-width 150 :on-action {:event/type :text.switch :id x}}) text-ids)}}
     :center {:on-key-pressed {:event/type :text.keypressed}
              :fx/type        :v-box
              :children       [(flow-pane (mapv member members))
                               (flow-pane (mapv cell colored-cells))]}}))