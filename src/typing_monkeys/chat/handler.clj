(ns typing-monkeys.chat.handler
  (:require [typing-monkeys.base :refer [handler *state]]
            [typing-monkeys.chat.db :as db]
            [typing-monkeys.chat.data :as data]
            [manifold.stream :as st])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(defn clear-input! []
  (swap! *state assoc-in [:chat :input] ""))

(defn room_stop! []
  (when-let [stop! (get-in @*state [:chat :room :stop!])]
    (stop!)))

(defn room_watch! [room-id]

  (room_stop!)

  (let [stream1 (db/room-stream room-id)
        stream2 (db/message-stream room-id)]

    (st/consume
     (fn [x]
       (println "room-upd " #_x)
       (swap! *state update-in [:chat :room] merge x))
     stream1)

    (st/consume
     (fn [x]
       (println "message-upd " #_x)
       (swap! *state assoc-in [:chat :room :messages] x))
     stream2)

    (swap! *state assoc-in [:chat :room :stop!]
           (fn close! []
             (st/close! stream1)
             (st/close! stream2)))))

(defn post-message! []
  (let [{user                 :user
         {:keys [input room]} :chat} @*state
        msg (data/message user input)]
    #_(println "posting message " msg)
    (swap! *state update-in [:chat :room :messages] conj msg)
    (db/room_add-message! room msg)
    (clear-input!)))

(defn init-state! []
  #_(println "logged in! ")
  (let [[id :as ids] (db/get-room-ids)
        room (db/get-room id)]

    (swap! *state
           (fn [state]
             (-> state
                 (assoc :page :chat)
                 (assoc-in [:chat :rooms] ids)
                 (assoc-in [:chat :room] room))))))

(defn enter-press? [{:keys [fx/event]}]
  (= KeyCode/ENTER (.getCode ^KeyEvent event)))

(defmethod handler :chat.init [{:keys [user]}]
  (println "init chat")
  #_(println "init chat"
           (let [[id :as ids] (db/get-room-ids)]
             (db/get-room id)))
  (let [[id :as ids] (db/get-room-ids)
        room (db/get-room id)]
    (handler {:event/type :chat.watch-room :id id})
    (swap! *state
           (fn [state]
             (-> (assoc state :module :chat)
                 (assoc-in [:chat :rooms] ids)
                 (assoc-in [:chat :room] room)
                 (assoc-in [:chat :pseudo] (:pseudo user)))))
    ))

(defmethod handler :chat.watch-room [{:keys [id]}]
  (room_watch! id))

(defmethod handler :chat.type [{:keys [fx/event]}]
  (swap! *state assoc-in [:chat :input] event))

(defmethod handler :chat.send [event]
  (when (enter-press? event) (post-message!)))

(defmethod handler :chat.swap-room [{:keys [room-id]}]
  (swap! *state assoc-in [:chat :room] (db/get-room room-id))
  (handler {:event/type :chat.watch-room :id room-id}))

(defmethod handler :chat.logout [_]
  (swap! *state dissoc :chat))