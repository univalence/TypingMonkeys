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

(defn context []
  (let [user (:out (clojure.java.shell/sh "whoami"))
        dir (last (clojure.string/split (:out (clojure.java.shell/sh "pwd")) #"/"))
        rem-carret-returns (fn [s] (clojure.string/replace s #"\n" ""))]
    {:dir (rem-carret-returns dir)
     :user (rem-carret-returns user)}))

(defn prompt-string
  ([]
   (let [{:keys [user dir]} (context)]
     (prompt-string user dir)))
  ([user dir]
   (str user ":" dir ">")))

(comment
  (deref (process '[cd src && pwd] {:out :string}))
  (clojure.java.shell/sh "pwd")
  (clojure.java.shell/sh "echo" "pouet")
  (do clojure.java.shell/*sh-dir*))
