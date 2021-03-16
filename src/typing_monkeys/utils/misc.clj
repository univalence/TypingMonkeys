(ns typing-monkeys.utils.misc
  (:require [clojure.walk :as walk]))

(defn pp [& xs]
  (mapv clojure.pprint/pprint xs) (last xs))

(defn walk_keyword->string
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (keyword? x) (name x) x)) m)))