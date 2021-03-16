(ns typing-monkeys.collaborative.simple-text.views
  (:require [cljfx.api :as fx]
            [xp.data-laced-with-history :as d]))

(defn component
  [{:keys [tree]}]
  (let [tree-seq (d/tree-seq tree)
        text (d/tree-seq->str tree-seq)]
    {:fx/type :stage
     :showing true
     :scene   {:fx/type :scene
               :root    {:fx/type  :grid-pane
                         :padding  10
                         :hgap     10
                         :children [{:fx/type :text-area
                                     :text    text}]}}}))

(def tree (reduce d/insert d/zero d/data))

(fx/on-fx-thread
 (fx/create-component (component {:tree tree})))