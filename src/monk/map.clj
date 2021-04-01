(ns monk.map
  (:require [monk.utils :as u :refer [defn+]]
            [monk.lenses-c :as l]
            [monk.flow :as flow])
  (:import (clojure.lang MapEntry)))

(defn+ km [& xs]
       (println 'km xs)
       (loop [todo xs ret {}]
         (if-not (seq todo)
           ret
           (let [[a & tail] todo]
             (cond (map-entry? a) (recur tail (l/upd ret (-> a key u/path l/path) (flow/trans (val a))))
                   (keyword? a) (recur (cons (MapEntry. a (first tail)) (next tail)) ret)
                   (or (map? a) (vector? a)) (recur (concat a tail) ret))))))


(km {:a 1}
    {:b 2 :e.f 8}
    [:c 2 :d 6 :b inc])

(comment :compile-time-optimization

         (defn split-km-body [xs]
           (loop [todo xs
                  current nil
                  elements []]
             (if-not (seq todo)
               (keep identity (if current (conj elements current) elements))
               (let [[a & tail] todo]
                 (cond
                   (keyword? a) (recur (next tail) (assoc current a (first tail)) elements)
                   (vector? a) (recur (concat a tail) current elements)
                   (map? a) (recur tail (km current a) elements)
                   :else (recur tail nil (conj elements current a)))))))

         (split-km-body '({:a 1}
                          {:b 2}
                          [:c 2 :d 6 :b inc])))

