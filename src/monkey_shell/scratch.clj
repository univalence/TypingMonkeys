(ns monkey-shell.scratch
  (:require [cljfx.api :as fx]))

(def *state (atom {:one {:showing true :text "one"}
                   :two {:showing false :text "two"}}))

(defn window [{:keys [showing text]}]
  {:fx/type :stage
   :always-on-top true
   :showing showing
   :scene {:fx/type :scene
           :root {:fx/type :label
                  :text text}}})

(defn root [state]
  ())

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))