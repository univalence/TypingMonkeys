(ns xp.cljfx.tut
  (:require [cljfx.api :as fx]))

(def fx-node-logger
  (into {}
        (mapv (fn [x]
                [(keyword x) (fn [_] (println x))])
              '[:on-context-menu-requested
                :on-drag-detected
                :on-drag-done
                :on-drag-dropped
                :on-drag-entered
                :on-drag-exited
                :on-drag-over
                :on-input-method-text-changed
                :on-key-pressed
                :on-key-released
                :on-key-typed
                :on-mouse-clicked
                :on-mouse-drag-entered
                :on-mouse-drag-exited
                :on-mouse-dragged
                :on-mouse-drag-over
                :on-mouse-drag-released
                :on-mouse-entered
                :on-mouse-exited
                :on-mouse-moved
                :on-mouse-pressed
                :on-mouse-released
                ;; :on-rotate
                ;; :on-rotat
                ;;:on-finished
                ;;:on-rotate
                ;;:on-started
                ;;:on-scroll-finished
                ;;:on-scroll
                ;;:on-scroll-started
                ;;:on-swipe-down
                ;;:on-swipe-left
                ;;:on-swipe-right
                ;;:on-swipe-up
                ;;:on-touch-moved
                ;;:on-touch-pressed
                ;;:on-touch-released
                ;;:on-touch-stationary
                ;;:on-zoom-finished
                ;;:on-zoom
                ;;:on-zoom-started
                ])))

(def renderer
  (fx/create-renderer))

(defn root [_]
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             :root    {:fx/type  :v-box
                       :padding  50
                       :children [(merge {:fx/type :button
                                          :text    "swap"}
                                         fx-node-logger)]}}})

(renderer {:fx/type root})

