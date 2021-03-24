(ns typing-monkeys.text.core
  (:require [cljfx.api :as fx]
            [typing-monkeys.text.state :as s]
            [typing-monkeys.text.db :as db]
            [typing-monkeys.text.views :as vs]
            [typing-monkeys.utils.misc :refer [pp]])
  (:import [javafx.scene.input KeyCode KeyEvent]
           (java.util UUID)))

(db/reset-first-text)

(def uuid (.toString (UUID/randomUUID)))

(def user
  #_(db/get-user "francois@univalence.io")
  (db/get-user "pierrebaille@gmail.com"))

(def *state
  (atom (s/make-state user
                      (db/get-text "first"))))

(db/watch-text "first"
               (fn [x]
                 (if-not (= uuid (get x :last-updater))
                   (let [new-state (s/make-state user x)]
                     (swap! *state (fn [old-state] (with-meta new-state (meta old-state)))))
                   #_(pp "do not reset state"))))

(defn swap!! [f & args]
  (let [v (apply swap! *state f args)]
    (future
     (db/sync-state! v uuid)
     (swap! *state vary-meta assoc :local-changes []))))

(defn map-event-handler [event]
  (case (:event/type event)
    :text.keypressed (condp = (.getCode ^KeyEvent (:fx/event event))
                       KeyCode/RIGHT (swap!! s/next-position)
                       KeyCode/LEFT (swap!! s/prev-position)
                       KeyCode/BACK_SPACE (swap!! s/delete-char)
                       (if-let [t (.getText ^KeyEvent (:fx/event event))]
                         (swap!! s/insert-text t)))
    nil))

(fx/mount-renderer
 *state
 (fx/create-renderer
  :middleware (fx/wrap-map-desc assoc :fx/type vs/root)
  :opts {:fx.opt/map-event-handler map-event-handler}))