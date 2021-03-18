(ns typing-monkeys.utils.misc
  (:require [clojure.walk :as walk]))

(defn pp [& xs]
  (mapv clojure.pprint/pprint xs) (last xs))

(defn walk_keyword->string
  [m]
  (let [f (fn [[k v]] (if (keyword? k) [(name k) v] [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (keyword? x) (name x) x)) m)))

(defn keywordize-keys
  "Recursively transforms all map keys from strings to keywords."
  {:added "1.1"}
  [m]
  (let [f (fn [[k v]] (if (string? k) [(keyword k) v] [k v]))]
    ;; only apply to maps
    (walk/postwalk (fn [x] (if (or (instance? java.util.HashMap x) (map? x)) (into {} (map f x)) x)) m)))