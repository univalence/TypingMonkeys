(ns xp.cljfx.layout-test
  (:require [cljfx.api :as fx]
            [typing-monkeys.utils.cljfx :refer [defc styled stylesheet]]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.style.constants :as sc]))

(defn rand-color []
  (rand-nth sc/colors))

(defn event [x & {:as opts}]
  (merge {:event/type :expr
          :verb       (first x)
          :subject    (second x)
          :args       (drop 2 x)
          :form       x}
         opts))

(defc label [text])

(defc h-box [& children]
      :spacing 5
      :padding 5)

(defc v-box [& children]
      :spacing 5
      :padding 5)

(defc stage [title scene]
      :showing true
      :title "test"
      :x 900
      :y -900
      :width 960
      :height 540)

(defn box [text & [color]]
  {:fx/type  :h-box
   :style    {:-fx-background-color (or color (rand-color))
              :-fx-alignment        :center
              :-fx-padding          30}
   :children [(label text)]})

(defc border-pane [])

(do :grid

    (defn grid-scaled-dimension [xs]
      (let [rat (/ 100 (reduce + xs))]
        (mapv #(* rat %) xs)))

    (defn grid [xs ys & {:as opts :keys [children]}]
      (merge {:fx/type            :grid-pane
              :padding            10
              :hgap               10
              :column-constraints (map (fn [x] {:fx/type :column-constraints :percent-width x}) (grid-scaled-dimension xs))
              :row-constraints    (map (fn [y] {:fx/type :row-constraints :percent-height y}) (grid-scaled-dimension ys))
              :children           (map (fn [[[x y] e]] (assoc e :grid-pane/column x :grid-pane/row y)) children)}
             (dissoc opts :children)))

    (defn grid-default-elements [x y]
      (into {} (for [x (range x) y (range y)]
                 [[x y] (label (str x "-" y)
                               :style {:-fx-background-color (rand-nth sc/colors)})])))

    (stage "test"
           {:fx/type :scene
            :root    (grid [1 9]
                           [2 8]
                           :children (grid-default-elements 2 2))}))

(defc button [text on-action])

(defn ebutton
  ([expr] (button (str expr) (event expr)))
  ([text expr] (button text (event expr))))

(def styles
  (stylesheet ::main
              ".button"
              {:text-fill        :gray
               :font-family      :monospace
               :font-weight      :bold
               :padding          2
               :margin           20
               :border-color     :gray
               :background-color :transparent
               :border-width     2
               :border-radius    4
               ":hover"          {:text-fill    :tomato
                                  :border-color :tomato}}

              ;; test
              ".notification" {:background-color :black
                               ":hover"          {:background-color :gray}

                               :>                {:text-fill        :white
                                                  :background-color :purple
                                                  ":hover"          {:background-color :yellow}
                                                  ".danger"         {:text-fill :red}
                                                  ".info"           {:text-fill :gray}}}
              ))

(pp (clojure.string/split (slurp (:cljfx.css/url styles)) #"\n"))

(defn root
  [{:as   state
    :keys [panels]}]
  (println "render" panels)
  (stage "root"
         {:fx/type     :scene
          :stylesheets [(:cljfx.css/url styles)]
          :root        (merge (border-pane) panels)}))

(defn notification [{:keys [text variant]}]
  {:fx/type     :v-box
   :style-class "notification"
   :children    [(merge {:fx/type :label
                         :text    text}
                        (when variant {:style-class variant}))]})


(def panels
  {:center (box "center")
   :top    {:fx/type  :h-box
            :alignment :top-right
            :children [(label "one") (label "two") (label "three")]}
   :bottom (box "bottom")
   :left   (box "left")
   :right  (v-box
            ;;:min-width 400
            (ebutton "grow" [:grow :right-pane])
            (ebutton "shrink" [:shrink :right-pane])
            (notification {:text "iop" :variant "danger"})

            (styled (button "third" {:event/type :foo})
                    :text-fill :purple
                    :border-color :purple))})

(def *state
  (atom {:panels panels}))

(defn put! [& xs] (apply swap! *state u/set xs))
(defn upd! [& xs] (apply swap! *state u/upd xs))



(defn handle [{:keys [fx/event verb subject args]}]
  (case subject
    :right-pane
    (case verb
      :shrink (put! :panels.right.min-width 200)
      :grow (put! :panels.right.min-width 600))))

(defn init []
  (def renderer
    (fx/create-renderer
     :middleware (fx/wrap-map-desc assoc :fx/type root)
     :opts {:fx.opt/map-event-handler handle}))

  (fx/mount-renderer *state renderer))

(do (init))
(renderer)

#_(pp @*state)
#_(renderer {:fx/type root})
