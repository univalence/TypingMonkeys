(ns xp.data-laced-with-history
  (:refer-clojure :exclude [isa atom tree-seq])
  (:require [xp.vec :as vec]))

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

(do :one

    ;; inserting new operation : 0(1)
    ;; building output array : O(n^2)

    (defn exec [v [id parent [op arg] :as o]]
      (if (empty? v)
        (case op
          :ins [[id arg]]
          :del (error "could not delete more"))
        (if-let [idx (index-where v (fn [[idx]] (= idx parent)) 0)]
          (case op
            :ins (vec/insert v (inc idx) [id arg])
            :del (assoc-in v [idx 1] nil))
          (error "no parent" v o))))

    (defn operations->str [ops]
      (let [x (reduce exec [] ops)]
        (->> (map second x)
             (remove nil?)
             (map name)
             (apply str)))))

(do :two

    ;; inserting new operation : O(n)
    ;; building output array : O(n)

    (defc tree [id op children])

    (def zero (tree [0 0] [:nop] []))

    (defn tree_insert-children
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
        (tree_insert-children tree event)
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

    (let [tree (reduce insert zero data)]
      (tree-seq tree)
      (tree->str tree))

    (defn last-id [{:as tree :keys [id children]}]
      (if-let [cs (seq children)]
        (last (sort-by second (map last-id cs)))
        id))

    )

(comment
 (tree-seq (reduce insert zero data)))



;; ----------------------------------------------------------------------------------------------------------------




(comment :xp1-irrelevant

         (defc insertion [site lstamp item index])

         (defc deletion [site lstamp index])

         (defn show [x]
           (tcond x
                  :insertion (list* 'ins (vals x))
                  :deletion (list* 'del (vals x))))

         (defn exec [v x]
           (println "exec " v x)
           (tcond x
                  :insertion (vec/insert v (:index x) (:item x))
                  :deletion (vec/put v (:index x) nil)))

         (let [i (insertion 0 1 \A 0)]
           (assert (and (= i {:site 0 :lstamp 1 :item \A, :index 0})
                        (isa i :insertion))))

         (let [f (fn [x]
                   (tcond x
                          :insertion :a
                          :deletion :b))
               i (insertion 0 0 \A 0)
               d (deletion 0 0 1)]
           (assert (and (= (f i) :a)
                        (= (f d) :b))))

         (def ins insertion)
         (def del deletion)

         (def data1
           [(ins 1 1 \C 0)
            (ins 1 2 \M 1)
            (ins 1 3 \D 2)
            (del 1 6 1)
            (ins 2 6 \D 3)
            (del 1 7 1)
            (ins 2 7 \E 4)
            (ins 3 7 \A 3)
            (ins 1 8 \T 1)
            (ins 2 8 \L 5)
            (ins 3 8 \L 4)
            (ins 1 9 \R 2)
            (ins 3 9 \T 5)
            (ins 1 10 \L 3)])


         (pp (map show (sort-by (juxt :lstamp :site) data1)))

         (reduce exec
                 []
                 (sort-by (juxt :lstamp :site) data1))

         (comment
          (exec "abcd" (ins 0 0 \A 2))
          (exec "" (ins 0 0 \A 0))
          (exec "abcd" (del 0 0 2))))