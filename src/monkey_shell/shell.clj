(ns monkey-shell.shell
  (:require [babashka.process :as pc :refer [$ check process pb pipeline]]
            [clojure.string :as str]
            [clojure.java.io :as io]
            [clojure.data :as data]))

(defn callback-writer [f]
  (proxy [java.io.Writer] []
    (write [s x len]
      (f (apply str (take len (slurp s)))))))

(defn whoami []
  (:out (clojure.java.shell/sh "whoami")))

(defn prompt-string
  ([cmd]
   (let [{:keys [user pwd]} cmd]
     (prompt-string user pwd)))
  ([user dir]
   (let [parent-dir (last (str/split dir #"/"))]
     (str/replace (str user ":" parent-dir ">") #"\n" ""))))

(defn env-exports [env]
  (concat
    (when-let [at (get env :PWD)] [(str "cd " at)])
    (map (fn [[k v]] (str (name k) "=" v)) env)))

(defn wrap-cmd [text env]
  ["bash" "-c"
   (clojure.string/join " && "
                        (concat (env-exports env)
                                ["DIR=$PWD" text "cd $DIR" "env > env.txt"]))])

(defn env-str->clj [s]
  (let [env (->> (clojure.string/split s #"\n")
                 (map (fn [x] (vec (next (re-matches #"([^=]*)=(.*)" x)))))
                 (into {}))]
    (assoc env
      "PWD"
      (get env "OLDPWD"))))

(defn execute
  [{:as cmd :keys [text env on-out on-done]}]
  (let [initial-env (System/getenv)
        cmd (wrap-cmd text env)
        ;; _ (pr cmd)
        p (process cmd
                   {:out (callback-writer on-out)})]
    (future
      (when (and @p on-done)
        (let [new-env (env-str->clj (slurp "env.txt"))
              [old new same] (data/diff initial-env new-env)]
          (on-done p new)
          @(process ["rm" "env.txt"]))))
    p))





(comment

  (process ["bash" "-c" "cd src && ls -la && env > env.txt"]
           {:out (callback-writer println)})

  (execute {:text "cd src && ls -la"
            :env {}
            :on-out (fn [r] (println "out " r))
            :on-done (fn [_ _] (println "ok"))}
           #_(fn [_ env] (clojure.pprint/pprint env)))

  (select-keys (.environment (:pb (pb '[])))
               )

  (deref (process '[env] {:out :string}))
  (clojure.java.shell/sh "pwd")
  (clojure.java.shell/sh "echo" "pouet")
  (clojure.java.shell/sh "bash" "-c" "cd src && ls -la && env > env.txt")
  (clojure.java.shell/sh "bash" "-c" "export RETURN=$(cd src && ls -la && echo pouet) && env >> env.txt && echo $RETURN")
  (clojure.java.shell/sh "bash" "-c" "cd src && ls -la && env >> env.txt")
  (deref (process ["bash" "-c" "cd src && ls -la && env >> env.txt"] {:inherit true}))
  ((pb ["bash" "-c" "cd src && ls -la"]))
  (deref (process ["bash" "-c" "cd src && ls -la"] {:out :string}))
  (do clojure.java.shell/*sh-dir*)

  (def p (pb '[]))
  (pc/start p)
  (do p)
  (process '[echo yo]))

()
