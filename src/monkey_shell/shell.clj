(ns monkey-shell.shell
  (:require [babashka.process :as pc :refer [check process pb pipeline]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn callback-writer [f]
  (proxy [java.io.Writer] []
    (write [s x len]
      (f (apply str (take len (slurp s)))))))


(defn execute [cmd f]
  (process cmd
           {:out (callback-writer f)}))
