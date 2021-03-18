(ns typing-monkeys.text.state
  (:require [xp.data-laced-with-history :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]])
  )

(do :transitions

    (defn tree-seq_first-visible [ts]
      (ffirst (drop-while (fn [[_ _ v]] (not v)) ts)))

    (defn tree-seq_next-site [ts]
      (last (sort (map ffirst ts))))

    (defn tree-seq_next-timestamp [ts]
      (inc (last (sort (map (comp second first) ts)))))

    (defn tree->state [tree]
      (let [ts (d/tree-seq tree)]
        {:tree      tree
         :position  (tree-seq_first-visible ts)
         :site-id   (tree-seq_next-site ts)
         :timestamp (tree-seq_next-timestamp ts)}))

    (defn adjacent-positions
      [{:as state :keys [tree position]}]
      (let [ts (d/tree-seq tree)
            visible? (fn [[_ _ visible]] visible)
            wrap-with-nil (fn [s] (concat (cons nil s) (list nil)))
            visible-positions (->> ts (filter visible?) (map first) wrap-with-nil)]

        (loop [triples (partition 3 1 visible-positions)]
          (if (seq triples)
            (let [[prev pos next] (first triples)]
              (if (= pos position)
                {:next next :prev prev}
                (recur (next pos))))
            (throw (Exception. "position not in tree-seq"))))))

    (defn next-position [{:as state :keys [tree position]}]
      (assoc state :position (:next (adjacent-positions state)))
      #_(let [nxt-pos (->> (d/tree-seq tree)
                         (drop-while (fn [[id _ _]] (not= id position)))
                         next
                         (drop-while (fn [[_ _ visible]] (not visible)))
                         first
                         first)]
        (assoc state
          :position (or nxt-pos position))))

    (defn prev-position [{:as state :keys [tree position]}]
      (assoc state :position (:prev (adjacent-positions state)))
      #_(let [prv-pos (->> (d/tree-seq tree)
                         (take-while (fn [[id _ _]] (not= id position)))
                         reverse
                         (drop-while (fn [[_ _ visible]] (not visible)))
                         first
                         first)]
        (assoc state
          :position (or prv-pos position))))

    (defn insert-text [{:as state :keys [position site-id timestamp]} t]
      (let [nxt-pos [site-id timestamp]]
        (-> state
            (update :timestamp inc)
            (assoc :position nxt-pos)
            (update :tree d/insert [nxt-pos position [:ins t]]))))

    (defn delete-char [{:as state :keys [position site-id timestamp]}]
      (-> state
          (update :tree d/insert [[site-id timestamp] position [:del]])
          prev-position)))