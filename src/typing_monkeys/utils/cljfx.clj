(ns typing-monkeys.utils.cljfx)

(defmacro defc
  "just some syntax for most common components"
  [nam fields & {:as default-opts}]
  (let [sym->kw (comp keyword name)
        fx-type {:fx/type (sym->kw nam)}
        fields (zipmap (map sym->kw fields) fields)]
    `(defn ~nam [~@fields ~'& {:as opts#}]
       (merge ~fx-type
              ~fields
              ~default-opts
              opts#))))
