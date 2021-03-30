(ns typing-monkeys.text2.handler
  (:require [typing-monkeys.base2 :refer [*state handler]]
            [typing-monkeys.text2.state :as s]
            [typing-monkeys.text2.db :as db])
  (:import [javafx.scene.input KeyCode KeyEvent]
           (java.util UUID)))

(defn swap!! [f & args]
  (let [v (apply swap! *state update :text f args)]
    (future
     (db/sync-state! (:text v) (:uuid v))
     (swap! *state update :text vary-meta assoc :local-changes []))))

(defmethod handler :text.keypressed [{:keys [fx/event]}]
  (when (= :text (:tab @*state))
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
        user-ref (-> user meta :ref)]
    (swap! *state assoc :uuid uuid
           :text (s/make-state user-ref
                               (db/get-text "first")))
    (db/watch-text "first"
                   (fn [x]
                     (if-not (= uuid (get x :last-updater))
                       (let [new-state (s/make-state user-ref x)]
                         (swap! *state update :text (fn [old-state] (with-meta new-state (meta old-state)))))
                       #_(pp "do not reset state"))))))
