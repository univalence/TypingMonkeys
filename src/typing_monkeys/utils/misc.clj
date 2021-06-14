(ns typing-monkeys.utils.misc
  (:refer-clojure :exclude [key get set])
  (:require [clojure.walk :as walk]
            [clojure.string :as string])
  (:import (java.util UUID)))

(defn uuid [] (str (UUID/randomUUID)))

(do :misc
    (def catv (comp vec concat))
    (def mapcatv (comp vec mapcat))
    (defn first-where [f xs] (first (filter f xs)))
    (defn mksym [& xs]
      (->> xs (map name) (apply str) symbol))
    (defn map-vals [f m]
      (into {} (map (fn [[k v]] [k (f v)]) m)))
    )

(do :test
    (defmacro is [x & xs]
      `(do (clojure.test/is ~x)
           (clojure.test/is (~'= ~x ~@xs))))

    (defmacro isnt [x & xs]
      `(clojure.test/is (~'= nil ~x ~@xs))))

(do :log-debug

    (defn pp [& xs]
      (mapv clojure.pprint/pprint xs) (last xs)))

(do :fn-syntax

    (defmacro f1
      ([pat expr] `(fn [~pat] ~expr))
      ([name pat expr] `(fn ~name [~pat] ~expr)))

    (defmacro f_
      ([expr] `(f1 ~'_ ~expr))
      ([name expr] `(f1 ~name ~'_ ~expr)))

    (defmacro fk
      ([keys expr] `(fn [{:keys ~keys}] ~expr))
      ([name keys expr] `(fn ~name [{:keys ~keys}] ~expr)))

    (defmacro defn+
      "behave the same as defn but will also define applied and underscore variations"
      [name & body]
      (let [name* (mksym name '*)
            name_ (mksym name '_)
            name_* (mksym name '_*)]
        `(do (declare ~name* ~name_ ~name_*)
             (defn ~name ~@body)
             (def ~name* (partial apply ~name))
             (defn ~name_ [& xs#] #(~name* % xs#))
             (def ~name_* (partial apply ~name_)))))

    (defn parse-fn [[fst & nxt :as all]]

      (let [[name fst & nxt]
            (if (symbol? fst)
              (cons fst nxt)
              (concat [nil fst] nxt))

            [doc fst & nxt]
            (if (string? fst)
              (cons fst nxt)
              (concat [nil fst] nxt))

            [opts fst & nxt]
            (if (map? fst)
              (cons fst nxt)
              (concat [{} fst] nxt))

            impls
            (if (vector? fst)
              {fst (vec nxt)}
              (into {}
                    (map
                     (fn [[args & body]]
                       [args (vec body)])
                     (cons fst nxt))))]

        (assoc opts
          :name name
          :doc doc
          :impls impls
          :cases (mapv (partial apply list*) impls))))

    (defmacro marked-fn

      "marked function,
       define an anonymous form (like fn)
       a def form (like defn)
       and a predicate function (like fn?)"

      [name & [doc]]

      `(do

         (defn ~(mksym "->" name) [f#]
           (vary-meta f# assoc ~(keyword name) true))

         (defmacro ~name
           ([f#] (list '~(mksym (str *ns*) "/->" name) f#))
           ([x# & xs#]
            (let [parsed# (parse-fn (cons x# xs#))]
              `(with-meta
                (fn ~(or (:name parsed#) (gensym)) ~@(:cases parsed#))
                {~~(keyword name) true}))))

         (defn ~(mksym name "?") [x#]
           (when (-> x# meta ~(keyword name)) x#))



         (defmacro ~(mksym 'def name) [name'# & body#]
           `(def ~name'# (~'~name ~@body#))))))

(do :def-syntax

    (defn pattern->symbols
      ([p] (pattern->symbols p []))
      ([p syms]
       (cond (symbol? p) (conj syms p)
             (coll? p) (catv syms (mapcatv pattern->symbols p)))))

    (defmacro lef
      "let + def
       let you def several symbols at once like let destructuration does
       (lef [a b c] [1 2 3]) -> (def a 1) (def b 2) (def c 3)"
      [pattern expr]
      `(let [~pattern ~expr]
         ~@(map (fn [s] `(def ~s ~s))
                (pattern->symbols pattern))))

    #_(lef {:keys [a b c] d :d [e1 e2 {:keys [i o p]} :as e] :e}
           {:a 1 :b 2 :c 3 :d 4
            :e [5 6 {:i 7 :o 8 :p 9}]})

    )

(do :path-and-keywords

    (defn dotsplit-keyword [k]
      (mapv keyword (string/split (name k) #"\.")))



    (defn path
      ([x]
       (cond (sequential? x) (mapcatv path x)
             (keyword? x) (dotsplit-keyword x)))
      ([x & xs]
       (mapcatv path (cons x xs))))

    (defn key [& xs]
      (keyword (string/join "." (map name (path xs)))))

    (assert (= :aze.bar.foo
               (key [:aze :bar] :foo)
               (key :aze :bar :foo)
               (key [:aze :bar :foo])
               (key :aze [:bar :foo])))

    (assert (= [:aze :bar :foo]
               (path [:aze :bar] :foo)
               (path :aze :bar :foo)
               (path [:aze :bar :foo])
               (path :aze [:bar :foo]))))

(do :get-set-upd

    (defn get
      ([x] x)
      ([x k]
       (if (fn? k)
         (k x)
         (get-in x (path k))))
      ([x k & ks]
       (reduce get (get x k) ks)))

    (defn upd
      ([x f] (f x))
      ([x at f]
       (update-in x (path at) f))
      ([x at f & xs]
       (reduce (fn [x [at f]] (upd x at f))
               (upd x at f)
               (partition 2 xs))))

    (defn set
      ([x at f]
       (assoc-in x (path at) f))
      ([x at f & xs]
       (reduce (fn [x [at f]] (set x at f))
               (set x at f)
               (partition 2 xs))))

    (def upd* (partial apply upd))
    (def set* (partial apply set))

    (defn sub-getter [p]
      (fn
        ([x] (get x p))
        ([x k] (get (get x p) k))))

    (defn sub-updater [p]
      (fn [x & xs]
        (upd x p (fn [x] (upd* x xs)))))

    (defn sub-setter [p]
      (fn [x & xs]
        (upd x p (fn [x] (set* x xs)))))

    (def M {:a 1
            :b {:c 2}})

    (dotsplit-keyword :b.c)
    (get M :a)
    (get M :b.c)
    ((sub-getter :b) M :c)
    (upd M :b.c inc)
    ((sub-updater :b) M :c inc)
    ((sub-setter :b) M :d 4 :e 5))

(do :threading

    (defmacro >_ [seed & xs]
      `(as-> ~seed ~'_ ~@xs)))




