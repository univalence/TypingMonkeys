(ns monkey-shell.core
  (:require [monkey-shell.state :as state :refer [*state]]
            [monkey-shell.events :as events]
            [monkey-shell.ui :as ui]
            [cljfx.api :as fx]))

(when-let [renderer (resolve 'renderer)]
  (println "unmounting")
  (fx/unmount-renderer *state renderer))

(events/init! "bastien@univalence.io")
#_(events/init! "pierrebaille@gmail.com")

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type ui/root)
    :opts {:fx.opt/map-event-handler events/handler}))

(defonce root
  (fx/mount-renderer
    *state
    renderer))


(comment
  (ui/members->true (state/get))
  (events/handler {:event/type :ui.popup.show})
  (renderer @*state)
  (:ui (state/get))
  (ui/root (state/get)))
