(ns typing-monkeys.base
  (:refer-clojure :exclude [set])
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [typing-monkeys.utils.misc :as u :refer [fk f1 lef]]))

;; state

(def *context
  (atom
   (fx/create-context
    {} cache/lru-cache-factory)))

(defonce *app
  (atom {:subs       {}
         :views      {}
         :events     {}
         :effects    {:fx/context (fx/make-reset-effect *context)
                      :dispatch   fx/dispatch-effect}
         :co-effects {:fx/context (fx/make-deref-co-effect *context)}}))

(defmulti event-multi :event/type)

(def event-handler
  (-> event-multi
      #_(fx/wrap-co-effects
         {:fx/context (fx/make-deref-co-effect *context)})
      #_(fx/wrap-effects
         {:fx/context (fx/make-reset-effect *context)
          :dispatch   fx/dispatch-effect})))

(defn event
  ([type data]
   (assoc data :event/type type))
  ([type k1 v1 & kvs]
   (event type (apply hash-map k1 v1 kvs))))

(defn registerer [type]
  (fn self
    ([xs] (self [] xs))
    ([from xs]
     (doseq [[k f] xs]
       (swap! *app update type assoc (u/key from k) f)))))

(def reg-views (registerer :views))
(def reg-events (registerer :events))
(def reg-effects (registerer :effects))
(def reg-co-effects (registerer :co-effects))

(defn initialize-event-multi! []
  (doseq [[k f] (:events @*app)]
    (.addMethod event-multi k f)))

(defn sub [ctx & xs] (apply fx/sub-val ctx u/get xs))
(defn set [ctx & xs] (apply fx/swap-context ctx u/set xs))
(defn upd [ctx & xs] (apply fx/swap-context ctx u/upd xs))

(defn module [key]
  (let [getter (u/sub-getter key)
        setter (u/sub-setter key)
        updater (u/sub-updater key)]
    {:key            (partial u/key key)
     :path           (partial u/path key)
     :sub            (fn [ctx & xs] (apply fx/sub-val ctx getter xs))
     :set            (fn [ctx & xs] (apply fx/swap-context ctx setter xs))
     :upd            (fn [ctx & xs] (apply fx/swap-context ctx updater xs))
     :event          (fn [k & xs] (apply event (u/key key k) xs))
     :reg-views      (partial reg-views key)
     :reg-events     (partial reg-events key)
     :reg-effects    (partial reg-effects key)
     :reg-co-effects (partial reg-co-effects key)}))

(defmacro defmodule [key]
  `(lef {:keys ~'[key path sub set upd event
                  reg-views reg-events reg-effects reg-co-effects]}
        (module ~key)))

#_(defn event-handler []
  (let [{:keys [effects co-effects]} @*app]
    (fn [{:as  event
          type :event/type}]
      ((effects type identity)
       ((co-effects type identity) event)))))

#_(defn compile-app []
  (let [handler ()
        renderer (create-renderer
                  :error-handler renderer-error-handler
                  :middleware (comp
                               wrap-context-desc
                               (wrap-map-desc desc-fn)
                               renderer-middleware)
                  :opts {:fx.opt/map-event-handler handler
                         :fx.opt/type->lifecycle   #(or (keyword->lifecycle %)
                                                        (fn->lifecycle-with-context %))})]
    (mount-renderer *context renderer)
    {:renderer renderer
     :handler  handler}))

(comment :deprecated

         ;; effects

         (defn reg-effects-fn
           ([xs] (reg-effects-fn [] xs))
           ([from xs]
            (doseq [[k f] (partition 2 xs)]
              (swap! *effects assoc (u/key from k) f))))

         (defmacro reg-effects
           [base-path & xs]
           `(do ~@(map (fn [[key argv & body]]
                         `(swap! *effects assoc (u/key ~base-path ~key)
                                 (fn ~argv ~@body)))
                       xs)))

         ;; events

         (defmulti event-handler-multi :event/type)

         (defn reg-events-fn
           ([xs] (reg-events-fn [] xs))
           ([from xs]
            (doseq [[k f] (partition 2 xs)]
              (.addMethod event-handler-multi (u/key from k) f))))

         (defmacro reg-events
           [base-path & xs]
           `(do ~@(map (fn [[key keys & body]]
                         `(defmethod event-handler-multi
                            (u/key ~base-path ~key)
                            [{:keys ~keys}]
                            ~@body))
                       xs)))



         (defmacro defv [module-key name pattern & body]
           `(defn ~name [x#] (let [~pattern (u/get x# ~module-key)] ~@body)))

         (defmacro module [key initial-data]
           `(do (refer-clojure :exclude '~'[get set key])
                (def ~'initial-data ~initial-data)
                (def ~'path (partial u/path ~key))
                (def ~'key (partial u/key ~key))
                (def ~'get (fn [ctx# x#] (fx/sub-val ctx# (u/sub-getter ~key) x#)))
                (def ~'set (fn [ctx# & xs#] (fx/swap-context ctx# (partial apply (u/sub-setter ~key)) xs#)))
                (def ~'upd (fn [ctx# & xs#] (fx/swap-context ctx# (partial apply (u/sub-updater ~key)) xs#)))
                (defmacro ~'reg-events ~'[& xs] `(typing-monkeys.base/reg-events ~~key ~@~'xs))
                (defmacro ~'reg-effects ~'[& xs] `(typing-monkeys.base/reg-effects ~~key ~@~'xs))
                (defmacro ~'defv ~'[& xs] `(typing-monkeys.base/defv ~~key ~@~'xs)))))