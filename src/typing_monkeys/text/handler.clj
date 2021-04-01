(ns typing-monkeys.text.handler
  (:require [typing-monkeys.base :refer [*state handler]]
            [typing-monkeys.text.state :as s]
            [typing-monkeys.text.db :as db]
            [typing-monkeys.utils.misc :as u :refer [pp]])
  (:import [javafx.scene.input KeyCode KeyEvent]
           (java.util UUID)))

(defn swap!! [f & args]
  (let [v (apply swap! *state update :text f args)]
    (future
     (swap! *state update :text vary-meta assoc :local-changes [])
     (db/sync-state! (:text v) (:uuid v)))))

(defmethod handler :text.keypressed [{:keys [fx/event]}]
  (pp 'keypressed event)
  (when (= :text (:module @*state))
    #_(println "keystroke " event)
    (let [cmd (.isMetaDown event)]
      (condp = (.getCode ^KeyEvent event)
        KeyCode/RIGHT (when cmd (swap!! s/next-position))
        KeyCode/LEFT (when cmd (swap!! s/prev-position))
        KeyCode/BACK_SPACE (swap!! s/delete-char)
        (if-let [t (when-not cmd (.getText ^KeyEvent event))]
          (swap!! s/insert-text t))))))

(defmethod handler :text.init [{:keys [user]}]
  (println "text-init " user)
  (let [uuid (.toString (UUID/randomUUID))
        user-ref (-> user meta :ref)
        text-ids (db/get-user-text-ids user-ref)]
    (swap! *state assoc
           :uuid uuid
           :module :text
           :text (s/make-state user-ref text-ids (db/get-text "first")))
    (db/watch-text (first text-ids)
                   (fn [x]
                     (if-not (= uuid (get x :last-updater))
                       (let [new-state (s/make-state user-ref text-ids x)]
                         (swap! *state update :text (fn [old-state] (with-meta new-state (meta old-state)))))
                       #_(pp "do not reset state"))))))
