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



