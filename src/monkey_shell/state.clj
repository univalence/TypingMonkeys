(ns monkey-shell.state
  (:refer-clojure :exclude [swap! get set!])
  (:require [clojure.core :as core]
            [monkey-shell.data :as data]
            [typing-monkeys.utils.misc :as u]))

(def *state (atom {}))

;; generic

(defn get
  ([] (deref *state))
  ([x] (u/get (get) x)))

(defn put!
  ([v] (reset! *state v))
  ([x v] (u/set (get) x v)))

(def swap!
  (partial core/swap! *state))

(defmacro swap!-> [& xs]
  `(swap! (fn [state#] (-> state# ~@xs))))

(defmacro swap!_ [& xs]
  `(swap! (fn [state#] (as-> state# ~'_ ~@xs))))

(defn upd!
  ([& xs] (put! (u/upd* (get) xs))))

;; specific

(defn with-new-session [state & [session-id]]
  (let [session-id (or session-id (str (gensym "shell_")))]
    (assoc state :session
                 (data/new-session session-id (:user state)))))

(defn with-focus [state id]
  (if-let [[session-id session-data]
           (or (find (:shell-sessions state) id)
               (first (:shell-sessions state)))]
    (assoc state :session (assoc session-data :id (name session-id)))
    (with-new-session state)))

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

  (assert (get [:p :o]) 43))

