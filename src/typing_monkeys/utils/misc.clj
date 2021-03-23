(ns typing-monkeys.utils.misc
  (:refer-clojure :exclude [key get set])
  (:require [clojure.walk :as walk]
            [clojure.string :as string]))

(do :misc
    (def catv (comp vec concat))
    (def mapcatv (comp vec mapcat)))

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
      ([name keys expr] `(fn ~name [{:keys ~keys}] ~expr))))

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




