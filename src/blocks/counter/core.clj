(ns blocks.counter.core
  (:require [clojure.core :as core]
            [monkey-shell.components.core :as comps]
            [cljfx.api :as fx]
            [blocks.state-sync :as sync]))

; ---- fx machinery ----

(comment :ex1

         (defn counter [state]
           (comps/hbox [(comps/squared-btn {:text "+"} :increment)
                        {:fx/type :label
                         :text (str (get state :count))}]))

         (def *state (sync/bind-document "scratch/compteur-1" {:count 0}))

         (defn root [state]
           {:fx/type :stage
            :showing true
            :scene {:fx/type :scene
                    :root {:fx/type :v-box
                           :children [(counter state)]}}})

         (defn handler
           [event]
           (case (:event/type event)
             :increment (swap! *state update :count inc)))

         (root (deref *state))

         (fx/mount-renderer
           *state
           (fx/create-renderer
             :middleware (fx/wrap-map-desc assoc :fx/type root)
             :opts {:fx.opt/map-event-handler handler})))

(defonce c1 (sync/bind-document "scratch/compteur-1" {:count 0}))
(defonce c2 (sync/bind-document "scratch/compteur-2" {:count 0}))
(defonce c3 (sync/bind-document "scratch/compteur-3" {:count 0}))

(def *state
  (sync/catom {:foo c1
               :bar c2
               :baz c3}))

(defn counter2 [id data]
  (comps/hbox [(comps/squared-btn {:text "+"}
                                  {:event/type :increment
                                   :id id})
               {:fx/type :label
                :text (str (get data :count))}]))

(defn named-counter [[id data]]
  (comps/hbox [{:fx/type :label :text (str id)}
               (counter2 id data)]))

(defn counter-map [state]
  (comps/vbox (mapv named-counter state)))

(defn root [state]
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [(counter-map state)]}}})

(defn handler
  [event]
  (println "handling: " event)
  (case (:event/type event)
    :increment (swap! *state update-in [(:id event) :count] inc)))

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler handler}))

(comment
  (deref *state))


