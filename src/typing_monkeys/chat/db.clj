(ns typing-monkeys.chat.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.chat.data :as data]
            [typing-monkeys.utils.walk :as walk]))


(defn get-room [room-id]
  (-> (f/coll db "rooms")
      (f/doc room-id)
      data/room-ref->data))

(defn get-room-ids []
  (->> (f/coll db "rooms")
       (f/docs)
       (map f/id)
       (sort)))

(defn room-stream [id]
  (f/->stream (f/doc db (str "rooms/" id))
              {:plain-fn (comp data/room-ref->data f/ref)}))

(defn message [user content]
  {:content   content
   :from      user
   :timestamp (System/nanoTime)})

(defn message-stream [room-id]
  (f/->stream (f/coll db (str "rooms/" room-id "/messages"))
              {:plain-fn (fn [x] (mapv (comp data/message-ref->data f/ref) x))}))

(defn room_create! [user-ref room-id]
  (f/create! (f/doc db (str "rooms/" room-id))
             {"members" [user-ref]})
  (f/add! (f/coll db (str "rooms/" room-id "/messages"))
          {"from" user-ref
           "content" (str "welcome to " room-id ".")}))

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

(defn room_add-message! [room message]
  (f/add! (f/coll db (str "rooms/" (:id room) "/messages"))
          (walk/stringify-keys (update message :from db/data->ref))))




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