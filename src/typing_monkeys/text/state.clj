(ns typing-monkeys.text.state
  (:require [typing-monkeys.text.crdt :as d]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.db :as db]))

(defn mk [user-ref text-ids {:as text :keys [tree members timestamp]}]
  (let [current-user? #(= user-ref (:user %))
        {:keys [color id position]} (first (filter current-user? members))]
    (db/with-ref
     (db/data->ref text)
     {:user          user-ref
      :tree          tree
      :position      position
      :color         color
      :member-id     id
      :timestamp     (inc timestamp)
      :members       (remove current-user? members)
      :text-ids      text-ids
      :local-changes []})))

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

(defn insert-text [{:as state :keys [position member-id timestamp]} t]
  (let [nxt-pos [member-id timestamp]
        operation [nxt-pos position [:ins t]]]
    (-> state
        (update :timestamp inc)
        (assoc :position nxt-pos)
        (update :tree d/insert operation)
        (update :local-changes conj operation))))

(defn delete-char [{:as state :keys [position member-id timestamp]}]
  (if (= [0 0] position)
    state
    (let [operation [[member-id timestamp] position [:del]]]
      (-> (prev-position state)
          (update :timestamp inc)
          (update :tree d/insert operation)
          (update :local-changes conj operation)))))