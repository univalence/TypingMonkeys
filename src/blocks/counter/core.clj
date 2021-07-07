(ns blocks.counter.core
  (:require [clojure.core :as core]
            [monkey-shell.components.core :as comps]
            [cljfx.api :as fx]
            [blocks.state-sync :as sync]))

; ---- fx machinery ----

(defn counter [state]
  (comps/hbox [(comps/squared-btn {:text "+"} :increment)
               {:fx/type :label
                :text (str (get state :count))}]))



(comment :ex1

         (def *state (sync/initialize-document "scratch/compteur" {:count 0}))

         (defn root [state]
           {:fx/type :stage
            :showing true
            :scene {:fx/type :scene
                    :root {:fx/type :v-box
                           :children [(counter state)]}}})

         (defn handler
           [event]
           (case (:event/type event)
             :increment (sync/swap! *state update :count inc)))

         (fx/mount-renderer
           *state
           (fx/create-renderer
             :middleware (fx/wrap-map-desc assoc :fx/type root)
             :opts {:fx.opt/map-event-handler handler})))

(defonce c1 (sync/create-document "scratch/compteur-1" {:count 0}))
(defonce c2 (sync/create-document "scratch/compteur-2" {:count 0}))
(defonce c3 (sync/create-document "scratch/compteur-3" {:count 0}))

(def *state
  (sync/ref-map {:foo c1
                 :bar c2
                 :baz c3}))

(defn named-counter [[name c]]
  (comps/hbox [{:fx/type :label :text (str name)}
               (counter c)]))

(defn counter-map [state]
  (comps/vbox (mapv named-counter state)))

(defn root [state]
  {:fx/type :stage
   :showing true
   :scene {:fx/type :scene
           :root {:fx/type :v-box
                  :children [(counter-map state)]}}})

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler (fn [_])}))
