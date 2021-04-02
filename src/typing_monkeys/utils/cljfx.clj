(ns typing-monkeys.utils.cljfx
  (:require #_[typing-monkeys.utils.misc :refer [pp] :as u]
   [cljfx.css :as css]
   [monk.utils :as u :refer [pp]]))

(comment :defc
         (defmacro defc
           "just some syntax for most common components"
           [nam fields & {:as default-opts}]
           (let [sym->kw (comp keyword name)
                 fx-type {:fx/type (sym->kw nam)}
                 field-map (zipmap (map sym->kw fields) fields)]
             `(defn ~nam [~@fields ~'& {:as opts#}]
                (merge ~fx-type
                       ~field-map
                       ~default-opts
                       opts#)))))
(comment :defc+

         (defn split-props [xs]
           (let [optseq (map vec (take-while (fn [[k _]] (keyword? k)) (partition 2 xs)))]
             {:opts (into {} optseq)
              :tail (drop (* 2 (count optseq)) xs)}))

         (defn field-pattern [xs]
           (let [xs (mapv (comp keyword name) xs)]
             (if (= '& (-> xs butlast last))
               {:fields   (-> xs butlast butlast)
                :variadic (last xs)}
               {:fields   xs
                :variadic :children})))

         (defn parse-instance-args

           [{:keys [fields variadic]} args]

           (let [sym->kw (comp keyword name)
                 nfields (count fields)
                 head-args (take nfields args)
                 {:keys [opts tail]} (split-props (drop nfields args))
                 base (merge (zipmap (map sym->kw fields) head-args) opts)]
             (if (seq tail)
               (assoc base variadic (vec tail))
               base)))

         (defmacro defc
           "just some syntax for most common components"
           [nam fields & {:as default-opts}]
           (let [pattern (field-pattern fields) sym->kw (comp keyword name)
                 base (merge {:fx/type (sym->kw nam)} default-opts)]
             `(defn ~nam [~'& xs#]
                (merge ~base
                       (parse-instance-args ~pattern xs#)))))

         (defmacro defcs [& body]
           `(do (map (fn [x] `(defc ~@x)) body)))

         (comment
          (macroexpand-1 '(defc h-box [min-width]))

          (defc h-box [min-width])
          (defc h-box [])

          (h-box {} {}))

         )

(do :misc

    (defn color->hex [c]
      (format "#%02X%02X%02X"
              (int (* 255 (.getRed c)))
              (int (* 255 (.getGreen c)))
              (int (* 255 (.getBlue c)))))
    )

(do :defc


    (defn km [& xs]
      (loop [todo xs ret {}]
        (if-not (seq todo)
          ret
          (let [[a & tail] todo]
            (cond (keyword? a) (recur (next tail) (assoc ret a (first tail)))
                  (map? a) (recur tail (merge ret a))
                  (nil? a) (recur tail ret))))))

    (defn split-body [xs]
      (loop [todo xs
             current nil
             elements []]
        (if-not (seq todo)
          (keep identity (if current (conj elements current) elements))
          (let [[a & tail] todo]
            (if (keyword? a)
              (recur (next tail) (assoc current a (first tail)) elements)
              (recur tail nil (conj elements current a)))))))

    (split-body '((iop iop iop) :a 1 :b g (pouet baz) :c pouet))



    (defn field-pattern [xs]
      (let [xs (mapv (comp keyword name) xs)]
        (if (= :& (-> xs butlast last))
          {:fields   (-> xs butlast butlast vec)
           :variadic (last xs)}
          {:fields xs})))



    (defmacro defc
      "just some syntax for most common components"
      [nam pattern & body]
      (let [{:keys [fields variadic]} (field-pattern pattern)
            type {:fx/type (-> nam name keyword)}
            opt-sym (gensym "opts_")
            pattern (if variadic pattern (conj (mapv u/mksym fields) '& {:as opt-sym}))
            base-expr (zipmap fields (map u/mksym fields))]
        `(defn ~nam ~pattern
           (km ~type
               ~@(split-body body)
               ~base-expr
               ~(if variadic
                  {variadic (u/mksym variadic)}
                  opt-sym)))))

    (defmacro defcs [& body]
      `(do (map (fn [x] `(defc ~@x)) body)))

    (defc button [text on-action])

    (comment
     (macroexpand-1 '(defc h-box [min-width]))
     (defc h-box [min-width & children])
     (h-box 1 {} {})
     (macroexpand-1 '(defc ext [a b]))
     (ext 1 2 :pouet 1 :foo.bar (fnil inc 0)))
    )

(do :styles

    (defmacro styles
      "build a style map for fx component
      it do nothing except prefixing every key with '-fx-' wich is painful to write"
      [& {:as styles}]
      (assert (every? keyword? (keys styles)))
      (zipmap (map (fn [k] (keyword (str "-fx-" (name k))))
                   (keys styles))
              (vals styles)))

    (defn child-styles [m]
      (assoc (into {} (filter (comp string? key) m))
         "*" (into {} (filter (comp keyword? key) m))))

    (defn prefix-properties
      [m]
      (into {}
            (map (fn [[k v]]
                   (cond
                     (= :> k) [">" (prefix-properties (child-styles v))]
                     (keyword? k) [(keyword (str "-fx-" (name k))) v]
                     (string? k) [k (prefix-properties v)]))
                 m)))

    (defmacro styled
      ([x ss]
       `(assoc ~x :style ~(prefix-properties ss)))
      ([x s & ss]
       `(assoc ~x :style ~(prefix-properties (apply hash-map s ss)))))

    (defn stylesheet [key & xs]
      (css/register key (prefix-properties (apply hash-map xs)))))



(do :constants
    (def builtins
      [;; cameras
       :parallel-camera
       :perspective-camera
       ;; charts
       :category-axis
       :number-axis
       :pie-chart-data
       :xy-chart-data
       :xy-chart-series
       :pie-chart
       :area-chart
       :bar-chart
       :bubble-chart
       :line-chart
       :scatter-chart
       :stacked-area-chart
       :stacked-bar-chart
       ;; effects
       :blend
       :bloom
       :box-blur
       :color-adjust
       :color-input
       :displacement-map
       :drop-shadow
       :gaussian-blur
       :glow
       :image-input
       :inner-shadow
       :lighting
       :light-distant
       :light-point
       :light-spot
       :motion-blur
       :perspective-transform
       :reflection
       :sepia-tone
       :shadow
       ;; scene
       :image-view
       :canvas
       :group
       :sub-scene
       :region
       :scene
       :stage
       ;; web
       :html-editor
       :web-view
       ;; media
       :media
       :media-player
       :media-view
       ;; panes
       :pane
       :anchor-pane
       :border-pane
       :flow-pane
       :grid-pane
       :row-constraints
       :column-constraints
       :h-box
       :stack-pane
       :text-flow
       :tile-pane
       :v-box
       ;; shapes
       :arc
       :circle
       :cubic-curve
       :ellipse
       :line
       :path
       :arc-to
       :close-path
       :cubic-curve-to
       :h-line-to
       :line-to
       :move-to
       :quad-curve-to
       :v-line-to
       :polygon
       :polyline
       :quad-curve
       :rectangle
       :svg-path
       :text
       ;; transform
       :affine
       :rotate
       :scale
       :shear
       :translate
       ;; 3d shapes
       :box
       :cylinder
       :mesh-view
       :triangle-mesh
       :sphere
       :ambient-light
       :point-light
       :phong-material
       ;; controls
       :popup
       :popup-control
       :context-menu
       :menu-item
       :check-menu-item
       :custom-menu-item
       :menu
       :radio-menu-item
       :tooltip
       :titled-pane
       :accordion
       :button-bar
       :choice-box
       :color-picker
       :combo-box
       :date-picker
       :button
       :check-box
       :hyperlink
       :menu-button
       :split-menu-button
       :toggle-button
       :toggle-group
       :radio-button
       :label
       :list-view
       :menu-bar
       :pagination
       :progress-indicator
       :progress-bar
       :scroll-bar
       :scroll-pane
       :separator
       :slider
       :spinner
       :integer-spinner-value-factory
       :double-spinner-value-factory
       :list-spinner-value-factory
       :split-pane
       :table-view
       :table-column
       :tab-pane
       :tab
       :text-area
       :text-field
       :text-formatter
       :password-field
       :tool-bar
       :tree-table-view
       :tree-item
       :tree-table-column
       :tree-view
       ;; cells
       :cell
       :date-cell
       :indexed-cell
       :list-cell
       :combo-box-list-cell
       :text-field-list-cell
       :table-cell
       :table-row
       :tree-cell
       :tree-table-cell
       :tree-table-row
       ;; dialogs
       :alert
       :choice-dialog
       :dialog
       :dialog-pane
       :text-input-dialog
       ;; transitions
       :fade-transition
       :fill-transition
       :parallel-transition
       :path-transition
       :pause-transition
       :rotate-transition
       :scale-transition
       :sequential-transition
       :stroke-transition
       :translate-transition]))