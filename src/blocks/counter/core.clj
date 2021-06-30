(ns blocks.counter.core
  (:require [clojure.core :as core]
            [monkey-shell.components.core :as comps]
            [cljfx.api :as fx]
            [blocks.state-sync :as sync]))

; ---- fx machinery ----

(def *state (sync/initialize-document "scratch/compteur" {:count 0}))

(defn counter [state]
  (comps/hbox [(comps/squared-btn {:text "+"} :increment)
               {:fx/type :label
                :text    (str (get state :count))}]))

(defn root [state] {:fx/type :stage
                    :showing true
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :v-box
                                        :children [(counter state)]}}})

(defn handler
  [event]
  (case (:event/type event)
    :increment (sync/swap! *state update :count inc)))

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler handler}))




