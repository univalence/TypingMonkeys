(ns blocks.counter.core
  (:refer-clojure :exclude [swap!])
  (:require [clojure.core :as core]
            [monkey-shell.components.core :as comps]
            [typing-monkeys.utils.misc :as utils]
            [cljfx.api :as fx]
            [firestore-clj.core :as f]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st])
  (:import (java.util HashMap ArrayList)))

(def initial-state {:count 0})

(defn new-state []
  (assoc initial-state :id (utils/uuid)))

(defn counter [state]
  (comps/hbox [(comps/squared-btn {:text "+"} :increment)
               {:fx/type :label
                :text    (str (get state :count))}]))

; ---- firebase machinery ----

(defn pull-walk [x]
  (cond
    (fu/ref? x)
    (fu/with-ref x (assoc (pull-walk (f/pull x)) :db/id (f/id x)))
    (fu/query? x)
    (fu/with-ref x (pull-walk (f/pull x)))
    (map? x) (into {} (map (fn [[k v]] [(keyword k) (pull-walk v)]) x))
    (vector? x) (mapv pull-walk x)
    (instance? HashMap x) (pull-walk (into {} x))
    (instance? ArrayList x) (pull-walk (into [] x))
    :else x))

(defonce db
         (f/client-with-creds "data/unsafe.json"))

(def scratch-coll
  (f/coll db "scratch"))

(def counter-group
  (pull-walk (f/doc scratch-coll "KLSbfwvytBPvc7BTX3zl")))

(defn new-state [id m]
  (-> (f/doc db id)
      (f/set! m)))

(comment

  (let [c1 (pull-walk (f/doc db "scratch/counter1"))
        c1' (update c1 :count inc)]
    (f/set! (fu/data->ref c1')
            (clojure.walk/stringify-keys c1')))

  (f/set! (f/doc db "scratch/counter1")
          {"count" 0
           "id"    "counter1"})

  (f/doc db "scratch/KLSbfwvytBPvc7BTX3zl")
  (f/add! scratch-coll
          {"counters" (vec (f/docs scratch-coll))})

  (let [id (utils/uuid)]
    (-> (f/doc scratch-coll id)
        (f/set! {"counters" []
                 "id"       id}))))

(defn watch! [x callback]
  (st/consume (fn [x]
                (let [data (pull-walk x)]
                  (callback data)))
              (f/->stream x)))

#_(watch! scratch-coll (fn [x]
                         (println "changed: " x)))

; ---- utils ----

(defn swap! [atom f & args]
  (let [ref (fu/data->ref @atom)
        new-state (apply core/swap! atom f args)]
    (f/set! ref (clojure.walk/stringify-keys new-state))))


(defn atomize
  "usage: (atomize \"scratch/counter1\")"
  [document-id]
  (-> (f/doc db document-id)
      (pull-walk)
      (atom)))

(def *counter1
  (atomize "scratch/counter1"))

(watch! (fu/data->ref @*counter1)
        (fn [x]
          (println "counter1 has changed: " x)
          (when (not= @*counter1 x)
            (core/reset! *counter1 x))))

(swap! *counter1
       (fn [c] (update c :count inc)))


; ---- fx machinery ----

(comment
  (defn root [state] {:fx/type :stage
                      :showing true
                      :scene   {:fx/type :scene
                                :root    {:fx/type  :v-box
                                          :children [(counter state)]}}})

  (def *state (atom (new-state)))

  (defn handler
    "HANDLER"
    [event]
    (case (:event/type event)
      :increment (swap! *state update :count inc)

      ))

  (fx/mount-renderer
    *state
    (fx/create-renderer
      :middleware (fx/wrap-map-desc assoc :fx/type root)
      :opts {:fx.opt/map-event-handler handler}))

  (println @*state))