(ns typing-monkeys.chat.handler
  (:require [typing-monkeys.chat.db :as db]
            [typing-monkeys.chat.data :as data]
            [manifold.stream :as st])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(defn chat-event
  "check if an event is an auth event, returns its simple name as a keyword if so"
  [{:as event
    t   :event/type}]
  (when (= "typing_monkeys.chat" (namespace t))
    (keyword (name t))))

(defn clear-input! [*state]
  (swap! *state assoc-in (data/path :input) ""))

(defn room_stop! [*state]
  (when-let [stop! (get-in @*state (data/path :room :stop!))]
    (stop!)))

(defn room_watch! [*state room-id]

  (room_stop! *state)

  (let [stream1 (db/room-stream room-id)
        stream2 (db/message-stream room-id)]

    (st/consume
     (fn [x]
       (println "room-upd " #_x)
       (swap! *state update-in (data/path :room) merge x))
     stream1)

    (st/consume
     (fn [x]
       (println "message-upd " #_x)
       (swap! *state assoc-in (data/path :room :messages) x))
     stream2)

    (swap! *state assoc-in (data/path :room :stop!)
           (fn close! []
             (st/close! stream1)
             (st/close! stream2)))))

(defn post-message! [*state]
  (let [{user :user
         {:keys [input room]} :chat} @*state
        msg (data/message user input)]
    #_(println "posting message " msg)
    (swap! *state update-in (data/path :room :messages) conj msg)
    (db/room_add-message! room msg)
    (clear-input! *state)))

(defn start! [*state]
  (println "logged in! ")
  (let [[id :as ids] (db/get-room-ids)
        room (db/get-room id)]
    (room_watch! *state id)
    (swap! *state
           (fn [state]
             (-> state
                 (assoc :page :chat)
                 (assoc-in (data/path :rooms) ids)
                 (assoc-in (data/path :room) room))))))

(defn enter-press? [event]
  (= KeyCode/ENTER (.getCode ^KeyEvent (:fx/event event))))

(defn event-handler
  "build an event handler for the auth component from the given *state (atom)"
  [*state]
  (fn [event]
    (when-let [event-name (chat-event event)]
      (case event-name
        :type (swap! *state assoc-in (data/path :input) (:fx/event event))
        :send (when (enter-press? event) (post-message! *state))
        :swap-room (swap! *state assoc (data/path :room) (db/get-room (:room-id event)))
        :logout (reset! *state data/ZERO)))))