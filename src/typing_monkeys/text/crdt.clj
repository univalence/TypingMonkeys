(ns typing-monkeys.text.crdt
  (:refer-clojure :exclude [isa atom tree-seq])
  (:require [monk.vec :as vec]))

(def about
  {:article-url "http://archagon.net/blog/2018/03/24/data-laced-with-history/"
   :notion-note "https://www.notion.so/univalence/Data-laced-with-history-6fc2300708be4d519c412d30559a3a9f"})

;; essai d'implementation des expériences situé dans la partie "Causal Trees"

(do :utils

    (defn pp [& xs]
      (println)
      (mapv clojure.pprint/pprint xs)
      (last xs))

    (def symbol->keyword
      (comp keyword name))

    (defn isa [x t]
      (-> x meta t))

    (defmacro defc [name fields]
      `(defn ~name ~fields
         (vary-meta ~(zipmap (mapv symbol->keyword fields) fields)
                    merge {~(symbol->keyword name) true})))

    (defmacro tcond [data & cases]
      `(condp #(isa %2 %1) ~data
         ~@cases))

    (defn index-where
      ([xs f] (index-where xs f 0))
      ([xs f offset]
       (when (seq xs)
         (if (f (first xs))
           offset
           (index-where (next xs) f (inc offset))))))

    (defn error [& xs]
      (throw (Exception. (with-out-str (apply pp xs))))))



;; inserting new operation : O(n)
;; building output array : O(n)

(defc tree [id op children])

(def zero (tree [0 0] [:nop] []))

(defn tree_insert-child-event
  [n [eid _ data]]
  (let [child (tree eid data [])]
    (update n
            :children
            (fn [xs]
              (if (empty? xs)
                [child]
                (let [idx (count (take-while (fn [{id :id}] (neg? (compare id eid))) xs))]
                  (vec/insert xs idx child)))))))

(defn insert [{:as tree :keys [id children]}
              [_ pid _ :as event]]
  (if (= id pid)
    (tree_insert-child-event tree event)
    (when-not (empty? children)
      (loop [[c1 & cs] children done []]
        (if-let [child (insert c1 event)]
          (assoc tree :children (vec/cat done (cons child cs)))
          (when cs (recur cs (conj done c1))))))))

(defn tree-seq
  ([tree] (tree-seq tree 0 []))
  ([{:keys [children op id]} at v]
   (let [verb (first op)
         v2 (case verb
              :del (vec/upd v (dec at) (fn [[id char _]] [id char false]))
              :ins (vec/insert v at [id (name (second op)) true])
              :nop v)
         at (if (= :ins verb) (inc at) at)]
     (reduce (fn [v child]
               (tree-seq child at v))
             v2 children))))

(defn tree-seq->str [n]
  (->> n
       (filter #(nth % 2))
       (map second)
       (apply str)))

(defn tree->str [t]
  (-> t tree-seq tree-seq->str))

(defn last-id [{:as tree :keys [id children]}]
  (if-let [cs (seq children)]
    (last (sort-by second (map last-id cs)))
    id))


















(defn tree_insert-child
  [n {:as child :keys [id]}]
  (update n
          :children
          (fn [xs]
            (if (empty? xs)
              [child]
              (let [idx (count (take-while (fn [c] (neg? (compare (:id c) id))) xs))]
                (vec/insert xs idx child))))))

(defn tree_remove-child [tree child-id]
  (update tree :children (fn [cs] (vec (remove #(= (:id %) child-id) cs)))))

(defn delete-node? [node]
  (= (:op node) [:del]))

(defn deletable-node [{:as tree :keys [id children]} limit]
  (and (if-not limit true (<= (second id) limit))
       (some delete-node? children)))

(defn mark-deleted [tree limit]
  (update (if (deletable-node tree limit)
            (assoc tree :deleted true)
            tree)
          :children
          (partial mapv #(mark-deleted % limit))))

(defn pop-deleted [tree]
  (let [children (mapv pop-deleted (:children tree))
        deleted-children (mapcat :children (filter :deleted children))
        children (vec (remove :deleted children))]
    (reduce tree_insert-child
            (assoc tree :children children)
            (remove delete-node? deleted-children))))

(defn compress [tree & [limit]]
  (pop-deleted (mark-deleted tree limit)))







(comment

 (def data
   [[[1 1] [0 0] [:ins :c]]
    [[1 2] [1 1] [:ins :m]]
    [[1 3] [1 2] [:ins :d]]
    [[1 6] [1 2] [:del]]
    [[2 6] [1 3] [:ins :d]]
    [[1 7] [1 3] [:del]]
    [[2 7] [2 6] [:ins :e]]
    [[3 7] [1 3] [:ins :a]]
    [[1 8] [1 1] [:ins :t]]
    [[2 8] [2 7] [:ins :l]]
    [[3 8] [3 7] [:ins :l]]
    [[1 9] [1 8] [:ins :r]]
    [[3 9] [3 8] [:ins :t]]
    [[1 10] [1 9] [:ins :l]]])

 (let [tree (reduce insert zero data)]
   (tree-seq tree)
   (tree->str tree))

 (tree-seq (reduce insert zero data))

 (def tree2 {:id       [0 0], :op [:nop],
             :children [{:id       [1 2], :op [:ins "a"],
                         :children [{:id       [1 3], :op [:ins "z"],
                                     :children [{:id       [1 4], :op [:ins "e"],
                                                 :children [{:id       [1 5], :op [:ins "r"],
                                                             :children [{:id       [1 6], :op [:ins "t"],
                                                                         :children [{:id       [1 7], :op [:ins "y"],
                                                                                     :children []}]}
                                                                        {:id       [1 8], :op [:del],
                                                                         :children []}]}]} {:id       [1 9], :op [:del],
                                                                                            :children []}]}]}]})
 (def tree3 {:id       [0 0], :op [:nop],
             :children [{:id       [1 2], :op [:ins "a"],
                         :children [{:id       [1 3], :op [:ins "z"],
                                     :children [{:id       [1 4], :op [:ins "e"],
                                                 :children [{:id       [1 5], :op [:del],
                                                             :children []}]}
                                                {:id       [1 6], :op [:del],
                                                 :children []}]}]}]})
 (pp tree2 (compress tree2))
 (pp tree3 (compress tree3)))

