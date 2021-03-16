
(ns typing-monkeys.chat.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]))

(defn user_ref->data [ref]
  (let [user-ref (f/pull-doc ref)]
    (db/with-ref ref
              {:id     (f/id ref)
               :pseudo (get user-ref "pseudo")})))

(defn message_ref->data [ref]
  (let [message-ref (f/pull-doc ref)]
    (db/with-ref ref
              {:id        (f/id ref)
               :content   (get message-ref "content")
               :from      (user_ref->data (get message-ref "from"))
               :timestamp (get message-ref "timestamp")})))

(defn room_ref->data [ref]
  (let [pulled (f/pull-doc ref)
        message-ref (f/coll ref "messages")
        members-ref (get pulled "members")]
    (db/with-ref ref
              {:id       (f/id ref)
               :messages (db/with-ref message-ref (mapv message_ref->data (f/docs message-ref)))
               :members  (db/with-ref members-ref (mapv user_ref->data members-ref))})))

(defn user_ref [email]
  (-> (f/coll db "users")
      (f/doc email)))

(defn room_ref [room-id]
  (-> (f/coll db "rooms")
      (f/doc room-id)))

(defn room_ids []
  (->> (f/coll db "rooms")
       (f/docs)
       (map f/id)
       (sort)))

(defn message [user content]
  {"content"   content
   "from"      user
   "timestamp" (System/nanoTime)})

(defn room_create! [user-ref room-id]
  (f/create! (f/doc db (str "rooms/" room-id))
             {"members" [user-ref]})
  (f/add! (f/coll db (str "rooms/" room-id "/messages"))
          (message user-ref (str "welcome to " room-id "."))))

(defn room_add-member! [room-ref user-ref]
  (let [members (vec (get (f/pull-doc room-ref) "members"))]
    (when-not (contains? (set members) user-ref)
      (f/merge! room-ref
                {"members" (conj members user-ref)}))))

(defn room_rem-member! [room-ref user-ref]
  (let [members (get (f/pull-doc room-ref) "members")]
    (when (contains? (set members) user-ref)
      (f/merge! room-ref
                {"members" (filter (partial = user-ref) members)}))))

(defn room_stop! [*state]
  (when-let [stop! (get-in @*state [:chat :room :stop!])]
    (stop!)))

(defn room_watch! [*state room-id]

  (room_stop! *state)

  (let [stream1
        (f/->stream (f/doc db (str "rooms/" room-id))
                    {:plain-fn identity})

        stream2
        (f/->stream (f/coll db (str "rooms/" room-id "/messages"))
                    {:plain-fn identity})]

    (st/consume
     (fn [x]
       (println "room-upd " #_x)
       (swap! *state update-in [:chat :room] merge (room_ref->data (f/ref x))))
     stream1)

    (st/consume
     (fn [x]
       (println "message-upd " #_x)
       (swap! *state assoc-in [:chat :room :messages] (mapv (comp message_ref->data f/ref) x)))
     stream2)

    (swap! *state assoc-in [:chat :room :stop!]
           (fn close! []
             (st/close! stream1)
             (st/close! stream2)))))

(defn room_post-message! [*state]
  (let [{user :user
         {:keys [input room]} :chat} @*state
        msg (message (db/data->ref user) input)]
    #_(println "posting message " msg)
    (swap! *state update-in [:chat :room :messages] conj msg)
    (f/add! (f/coll db (str "rooms/" (:id room) "/messages"))
            msg)))


(comment :scratch

         (do
           (room_create! (f/doc db "users/pierrebaille@gmail.com") "lobby")

           (room_add-member! (f/doc db "rooms/secondary")
                             (f/doc db "users/francois@univalence.io"))

           (room_add-member! (f/doc db "rooms/room3")
                             (f/doc db "users/bastien.guihard@univalence.io"))

           (room_create! (f/doc db "users/pierrebaille@gmail.com") "room6")
           (def a1 (atom {}))
           (def close (room_watch! a1 "room6"))
           (comment (close))
           (do @a1))

         (f/doc db "users/pierrebaille@gmail.com")
         (f/doc db "users/bastien.guihard@univalence.io")

         (f/pull-doc (f/doc db "rooms/room3"))

         (room_add-member! (f/doc db "rooms/room3")
                           (f/doc db "users/bastien.guihard@univalence.io"))
         (create-room! (f/doc db "users/pierrebaille@gmail.com")
                       "r3"))