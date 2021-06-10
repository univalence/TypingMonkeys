(ns monkey-shell.shell
  (:require [babashka.process :as pc :refer [check process pb pipeline]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

(defn callback-writer [f]
  (proxy [java.io.Writer] []
    (write [s x len]
      (f (apply str (take len (slurp s)))))))


(defn execute [cmd on-out on-dead]
  (let [p (atom nil)]
    (reset! p (process cmd
                       {:out (callback-writer
                               (fn [out]
                                 (on-out out)
                                 (future #_(Thread/sleep 1000)
                                         (when-not (.isAlive (:proc @p))
                                           (on-dead)))))}))))


