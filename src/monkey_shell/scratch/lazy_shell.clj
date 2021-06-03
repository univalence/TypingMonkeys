(ns monkey-shell.lazy-shell
  (:require [babashka.process :as pc :refer [check process pb]]
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


