
(ns typing-monkeys.text.core
  (:require [cljfx.api :as fx]
            [typing-monkeys.text.state :as s]
            [typing-monkeys.text.db :as db])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(def *state
  (atom (s/tree->state (db/get-first-tree))))

(defn map-event-handler [event]
  (case (:event/type event)
    ::press (condp = (.getCode ^KeyEvent (:fx/event event))
              KeyCode/RIGHT (swap! *state s/next-position)
              KeyCode/LEFT (swap! *state s/prev-position)
              KeyCode/BACK_SPACE (swap! *state s/delete-char)
              (if-let [t (.getText ^KeyEvent (:fx/event event))]
                (swap! *state s/insert-text t)))
    nil))

(fx/mount-renderer
 *state
 (fx/create-renderer
  :middleware (fx/wrap-map-desc assoc :fx/type vs/root)
  :opts {:fx.opt/map-event-handler map-event-handler}))