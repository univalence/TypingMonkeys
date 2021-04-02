(ns typing-monkeys.text.handler
  (:require [typing-monkeys.base :refer [*state handler client-id]]
            [typing-monkeys.text.state :as s]
            [typing-monkeys.text.db :as db]
            [typing-monkeys.utils.misc :as u :refer [pp]])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(defn swap!! [f & args]
  (let [{:keys [text]} (apply swap! *state update :text f args)]
    (future (db/sync-state! text)
            (swap! *state assoc-in [:text :local-changes] []))))

(defmethod handler :text.keypressed [{:keys [fx/event]}]
  (pp 'keypressed event)
  (when (= :text (:module @*state))
    (condp = (.getCode ^KeyEvent event)
      KeyCode/RIGHT (swap!! s/next-position)
      KeyCode/LEFT (swap!! s/prev-position)
      KeyCode/BACK_SPACE (swap!! s/delete-char)
      (if-let [t (.getText ^KeyEvent event)]
        (swap!! s/insert-text t)))))

(defmethod handler :text.init [{:keys [user]}]
  (println "text-init " user)
  (let [user-ref (-> user meta :ref)
        text-ids (db/get-user-text-ids user-ref)]
    (swap! *state assoc
           :text (s/mk user-ref text-ids (db/get-text (first text-ids))))
    (pp @*state)
    (db/watch-text! (first text-ids)
                    (fn [x]
                     (if-not (= client-id (get x :last-client))
                       (let [new-state (s/mk user-ref text-ids x)]
                         (swap! *state update :text (fn [{:as old-state :keys [local-changes]}] (assoc new-state :local-changes local-changes))))
                       (pp "do not reset state"))))))
