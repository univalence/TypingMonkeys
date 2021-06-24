(ns monkey-shell.core
  (:require [monkey-shell.state :as state :refer [*state]]
            [monkey-shell.events :as events]
            [monkey-shell.ui :as ui]
            [cljfx.api :as fx]
            [monkey-shell.data :as data]))

(def running (atom nil))

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type ui/root)
    :opts {:fx.opt/map-event-handler events/handler}))

(defn go []
  (when-not @running
    (events/init! "pierrebaille@gmail.com")
    #_(events/init! "bastien@univalence.io")
    (fx/mount-renderer *state #'renderer)
    (reset! running true)))

(go)

(comment
  (events/handler {:event/type :ui.popup.show})
  (renderer (state/get))
  (count (get-in (state/get) [:shell-sessions (get (state/get) :focused-session) :pending])))
