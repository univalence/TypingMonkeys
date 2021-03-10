(ns utils.misc)

(defn pp [& xs]
  (mapv clojure.pprint/pprint xs) (last xs))