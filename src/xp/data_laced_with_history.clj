(ns xp.data-laced-with-history
  (:refer-clojure :exclude [isa atom])
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


;;

(def o vector)

(def data
  [(o [1 1] [0 0] [:ins :c])
   (o [1 2] [1 1] [:ins :m])
   (o [1 3] [1 2] [:ins :d])
   (o [1 6] [1 2] [:del])
   (o [2 6] [1 3] [:ins :d])
   (o [1 7] [1 3] [:del])
   (o [2 7] [2 6] [:ins :e])
   (o [3 7] [1 3] [:ins :a])
   (o [1 8] [1 1] [:ins :t])
   (o [2 8] [2 7] [:ins :l])
   (o [3 8] [3 7] [:ins :l])
   (o [1 9] [1 8] [:ins :r])
   (o [3 9] [3 8] [:ins :t])
   (o [1 10] [1 9] [:ins :l])])

(do :one

    (defn exec [v [id parent [op arg] :as o]]
      #_(pp 'exec o 'on v)
      (if (empty? v)
        (case op
          :ins [[id arg]]
          :del (error "could not delete more"))
        (if-let [idx (index-where v (fn [[idx]] (= idx parent)) 0)]
          (case op
            :ins (vec/insert v (inc idx) [id arg])
            :del (assoc-in v [idx 1] nil))
          (error "no parent" v o))))

    (let [x (reduce exec [] data)]
      (->> (map second x)
           (remove nil?)
           (map name)
           (apply str))))



(do :two

    (defc node [id op children])

    (def zero (node [0 0] [:nop] []))

    (defn node_insert-children [n [eid _ data]]
      #_(println "noe ins ch" node eid data)
      (let [child (node eid data [])]
        #_(println "child " child)
        (update n
                :children
                (fn [xs]
                  #_(println "update-child")
                  (if (empty? xs)
                    [child]
                    (let [idx (count (take-while (fn [{id :id}] (neg? (compare id eid))) xs))]
                      (vec/insert xs idx child)))))))

    (defn insert [{:as node :keys [id children]}
                  [_ pid _ :as event]]
      (if (= id pid)
        (node_insert-children node event)
        (when-not (empty? children)
          (loop [[c1 & cs] children done []]
            (if-let [child (insert c1 event)]
              (assoc node :children (vec/cat done (cons child cs)))
              (when cs (recur cs (conj done c1))))))))

    (def red (reduce insert zero data))

    #_(pp red)

    (defn node->vec
      ([node] (node->vec node 0 []))
      ([{:keys [children op]} at v]
       #_(println v op at)
       (let [verb (first op)
             v2 (case verb
                  :del (vec/put v (dec at) nil)
                  :ins (vec/insert v at (second op))
                  :nop v)
             at (if (= :ins verb) (inc at) at)]
         (reduce (fn [v child]
                   (node->vec child at v))
                 v2 children))))

    #_(node->vec red)
    )



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