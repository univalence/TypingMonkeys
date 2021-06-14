(ns monkey-shell.state
  (:refer-clojure :exclude [swap! get set!])
  (:require [clojure.core :as core]
            [monkey-shell.data :as data]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.utils.firestore :as fu]))

(def *state (atom {}))

;; generic

(defn get
  ([] (deref *state))
  ([x] (u/get (get) x)))

(defn put!
  ([v] (reset! *state v))
  ([x v] (put! (u/set (get) x v))))

(def swap!
  (partial core/swap! *state))

(defmacro swap!-> [& xs]
  `(swap! (fn [state#] (-> state# ~@xs))))

(defmacro swap!_ [& xs]
  `(swap! (fn [state#] (as-> state# ~'_ ~@xs))))

(defn upd!
  ([& xs] (put! (u/upd* (get) xs))))

;; try

(comment

  (swap!_
    (merge _ {:foo :bar})
    (assoc-in _ [:p :o] 42))

  (swap!_
    (assoc _ :a 1)
    (update _ :a inc)
    (merge _ {:b 2 :c 3}))

  (get)
  (get :a)

  (upd! [:p :o] inc)

  (get)

  (assert (get [:p :o]) 43)

  (put! [:a :b :c] 1))

