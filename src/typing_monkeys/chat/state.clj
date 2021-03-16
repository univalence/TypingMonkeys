(ns typing-monkeys.chat.state
  (:require [typing-monkeys.utils.misc :as u]
            [typing-monkeys.chat.db :as db]
            [typing-monkeys.auth :as auth])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(def zero
  {:user nil
   :chat {:room  nil
          :input ""}
   :auth {:email   ""
          :passord ""}})

(def *state
  (atom zero))

(defn log []
  (u/pp @*state))

(defn clear-input! []
  (swap! *state assoc-in [:chat :input] ""))

(defn with-user [state email]
  (when-let [user (db/user_ref email)]
    (assoc state :user (db/user_ref->data user))))

(defn with-room [state room-id]
  (when-let [room (db/room_ref room-id)]
    (assoc-in state [:chat :room] (db/room_ref->data room))))

(defn send-message! []
  (db/room_post-message! *state)
  (clear-input!))

(defn on-login! [email]
  (println "logged in! ")
  (let [[room1 :as rooms] (db/room_ids)]
    (db/room_watch! *state room1)
    (swap! *state
           (fn [state]
             (-> (dissoc state :auth)
                 (assoc :page :chat)
                 (assoc-in [:chat :rooms] rooms)
                 (with-user email)
                 (with-room room1))))))

(defn enter-press? [event]
  (= KeyCode/ENTER (.getCode ^KeyEvent (:fx/event event))))

(defn event-handler [event]
  #_(println "event " event)
  (let [etype (:event/type event)]
    (when (= "typing_monkeys.chat.event" (namespace etype))
      (case (keyword (name etype))

        :type (swap! *state assoc-in [:chat :input] (:fx/event event))
        :send (when (enter-press? event) (send-message!))

        :swap-room (swap! *state with-room (:room-id event))

        :logout (reset! *state zero)

        :type-password (swap! *state assoc-in [:auth :password] (:fx/event event))
        :type-email (swap! *state assoc-in [:auth :email] (:fx/event event))

        :authenticate (let [{:keys [email password]} (:auth @*state)]
                        (auth/sign-in email password
                                      (fn [_] (on-login! email))
                                      (fn [e] (u/pp :error e))))))))