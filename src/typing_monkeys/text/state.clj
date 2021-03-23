(ns typing-monkeys.text.state
  (:require [xp.data-laced-with-history :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.db :as db])
  )

(do :transitions

    (defn tree-seq_first-visible [ts]
      (ffirst (drop-while (fn [[_ _ v]] (not v)) ts)))

    (defn tree-seq_next-site [ts]
      (last (sort (map ffirst ts))))

    (defn tree-seq_next-timestamp [ts]
      (inc (or (last (sort (map (comp second first) ts))) 0)))

    (defn make-state [user-ref {:as text :keys [tree members]}]
      (let [ts (d/tree-seq tree)
            {:keys [color id position]} (first (filter #(= user-ref (:user %)) members))]
        (db/with-ref
         (db/data->ref text)
         {:user      user-ref
          :tree      tree
          :position  position
          :color     color
          :site-id   id
          :timestamp (tree-seq_next-timestamp ts)
          :members members})))

    (defn adjacent-positions
      [{:as state :keys [tree position]}]
      (let [ts (cons [[0 0] "" true] (d/tree-seq tree))
            visible? (fn [[_ _ visible]] visible)
            wrap-with-nil (fn [s] (concat (cons nil s) (list nil)))
            visible-positions (->> ts (filter visible?) (map first) wrap-with-nil)]

        (loop [triples (partition 3 1 visible-positions)]
          (if (seq triples)
            (let [[prev pos nxt] (first triples)]
              (if (= pos position)
                {:next nxt :prev prev}
                (recur (next triples))))
            (throw (Exception. "position not in tree-seq"))))))

    (defn next-position [{:as state :keys [tree position]}]
      (assoc state :position (or (:next (adjacent-positions state))
                                 position)))

    (defn prev-position [{:as state :keys [tree position]}]
      (assoc state :position (or (:prev (adjacent-positions state))
                                 position)))

    (defn insert-text [{:as state :keys [position site-id timestamp]} t]
      (let [nxt-pos [site-id timestamp]
            operation [nxt-pos position [:ins t]]]
        (-> state
            (update :timestamp inc)
            (assoc :position nxt-pos)
            (update :tree d/insert operation)
            (vary-meta update :local-changes (fnil conj []) operation))))

    (defn delete-char [{:as state :keys [position site-id timestamp]}]
      (when-not (= [0 0] position)
        (let [operation [[site-id timestamp] position [:del]]]
          (-> state
              prev-position
              (update :tree d/insert operation)
              (vary-meta update :local-changes (fnil conj []) operation))))))