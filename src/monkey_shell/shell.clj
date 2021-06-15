(ns monkey-shell.shell
  (:require [babashka.process :as pc :refer [$ check process pb pipeline]]
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
                                 (future (when-not (.isAlive (:proc @p))
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

(def PERSISTED_ENV_KEYS
  ["PWD" "USER" "LOGNAME"])

(defn wrap-cmd [s]
  ["bash" "-c" (str "DIR=$PWD && " s " && cd $DIR && env > env.txt")])

(defn env-str->clj [s]
  (into {} (map (fn [x] (vec (next (re-matches #"(.*)=(.*)" x))))
        (clojure.string/split s #"\n"))))

(defn execute2
  ([cmd]
   (execute2 cmd println pr))
  ([cmd on-out on-dead]
   (let [p (process (wrap-cmd cmd)
                    {:out (callback-writer
                            (fn [out]
                              (on-out out)))})]
     (future @p (on-dead (env-str->clj (slurp "env.txt")))))))




(comment

  (execute2 "cd src && pwd"
            println
            (comp println count keys))

  (select-keys (.environment (:pb (pb '[])))
               ["PWD" "USER" "LOGNAME"])

  (deref (process '[env] {:out :string}))
  (clojure.java.shell/sh "pwd")
  (clojure.java.shell/sh "echo" "pouet")
  (clojure.java.shell/sh "bash" "-c" "cd src && ls -la")
  (clojure.java.shell/sh "bash" "-c" "export RETURN=$(cd src && ls -la && echo pouet) && env >> env.txt && echo $RETURN")
  (clojure.java.shell/sh "bash" "-c" "cd src && ls -la && env >> env.txt")
  (deref (process ["bash" "-c" "cd src && ls -la && env >> env.txt"] {:inherit true}))
  ((pb ["bash" "-c" "cd src && ls -la"]))
  (execute ["bash" "-c" "cd src && ls -la"]
           (fn [_] (println (System/getenv "PWD")))
           (fn [_]))
  (deref (process ["bash" "-c" "cd src && ls -la"] {:out :string}))
  (do clojure.java.shell/*sh-dir*)

  (def p (pb '[]))
  (pc/start p)
  (do p)
  (process '[echo yo]))

()
