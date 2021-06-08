(ns monkey-shell.scratch.lazy-shell
  (:require [babashka.process :as pc :refer [check process pb pipeline]]
            [clojure.string :as str]
            [clojure.java.io :as io]))

#_(process ["ls" "-la"] {:out :string})

#_(-> (process '[tail -f "note.org"] {:out :inherit})
      :out type)

(def on (atom true))

#_(future
    (loop []
      (spit "pouet.txt" (str (rand-int 10) "\n") :append true)
      (Thread/sleep 1000)
      (if @on (recur))))

(def foo (pc/pipeline
           #_(pc/pb '[tail -f "pouet.txt"])
           (pc/pb '[cat "pouet.txt"])
           (pc/pb '[grep "5"] {:out :string})))

(-> foo last deref)

(defn callback-writer [f]
  (proxy [java.io.Writer] []
    (write [s x len]
      (f (apply str (take len (slurp s)))))))


(def p (process '[tail -f "foo.txt"]
                {:out (callback-writer (fn [x] (println x)))}))

(process '[tail -f "foo.txt"]
         {:out :inherit})

(process ["ls"]
         {:out (callback-writer (fn [x] (println x)))})

(deref a)
(:out p)

(require '[babashka.process :refer [pipeline pb]])
(future
  (loop []
    (spit "log.txt" (str (rand-int 10) "\n") :append true)
    (Thread/sleep 1000)
    (recur)))
(-> (pipeline (pb '[tail -f "log.txt"])
              (pb '[cat])
              (pb '[grep "5"] {:out *out*}))
    last
    ;deref
    )

()

(process ["ls"] {:inherit true})