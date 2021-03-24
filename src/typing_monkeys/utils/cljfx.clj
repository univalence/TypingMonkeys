(ns typing-monkeys.utils.cljfx)

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
              opts#))))

(defmacro styles
  "build a style map for fx component
  it do nothing except prefixing every key with '-fx-' wich is painful to write"
  [& {:as styles}]
  (assert (every? keyword? (keys styles)))
  (zipmap (map (fn [k] (keyword (str "-fx-" (name k))))
               (keys styles))
          (vals styles)))

(defmacro styled [x & ss]
  `(assoc ~x :style (styles ~@ss)))
