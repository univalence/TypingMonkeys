(ns chat.db
  (:require [firestore-clj.core :as f]
            [utils.firestore :as fu]
            [manifold.stream :as st]))

(defonce db (f/client-with-creds
             "data/conatus-ef5f3-firebase-adminsdk-mowye-1aaf077bc3.json"))

(defn with-ref [ref data]
  (vary-meta data assoc :ref ref))

(defn data->ref [x]
  (-> x meta :ref))

(defn user_ref->data [ref]
  (let [user-ref (f/pull-doc ref)]
    (with-ref ref
              {:id     (f/id ref)
               :pseudo (get user-ref "pseudo")})))

(defn message_ref->data [ref]
  (let [message-ref (f/pull-doc ref)]
    (with-ref ref
              {:id        (f/id ref)
               :content   (get message-ref "content")
               :from      (user_ref->data (get message-ref "from"))
               :timestamp (get message-ref "timestamp")})))

(defn room_ref->data [ref]
  (let [pulled (f/pull-doc ref)
        message-ref (f/coll ref "messages")
        members-ref (get pulled "members")]
    (with-ref ref
              {:id       (f/id ref)
               :messages (with-ref message-ref (mapv message_ref->data (f/docs message-ref)))
               :members  (with-ref members-ref (mapv user_ref->data members-ref))})))

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
        msg (message (data->ref user) input)]
    (println "posting message " msg)
    (swap! *state update-in [:chat :room :messages] conj msg)
    (f/add! (f/coll db (str "rooms/" (:id room) "/messages"))
            msg)))



(comment

 (room_post-message! "room6"
                     (message (f/doc db "users/bastien.guihard@univalence.io")
                              "tic tac"))
 (do
   (room_create! (f/doc db "users/pierrebaille@gmail.com") "room6")
   (def a1 (atom {}))
   (def close (room_watch! a1 "room6"))
   (comment (close))
   (do @a1)))



(comment

 (f/doc db "users/pierrebaille@gmail.com")
 (f/doc db "users/bastien.guihard@univalence.io")

 (f/pull-doc (f/doc db "rooms/room3"))

 (room_add-member! (f/doc db "rooms/room3")
                   (f/doc db "users/bastien.guihard@univalence.io"))
 (create-room! (f/doc db "users/pierrebaille@gmail.com")
               "r3"))

(comment :old

         (require '[manifold.stream :as st])

         (fu/overide-print-methods)

         (defonce db (f/client-with-creds
                      "data/conatus-ef5f3-firebase-adminsdk-mowye-1aaf077bc3.json"))

         (defn watch-coll! [kw on-change]
           (st/consume
            on-change
            (f/->stream (f/coll db (name kw))
                        {:plain-fn vec})))


         (defn user-data [doc]
           (let [user (fu/pull-doc doc)]
             (with-meta {:id     (f/id doc)
                         :pseudo (get user "pseudo")}
                        {:doc doc})))

         (defn message-data [doc]
           (let [message (fu/pull-doc doc)]
             (with-meta {:id        (f/id doc)
                         :content   (get message "content")
                         :from      (user-data (get message "from"))
                         :timestamp (get message "timestamp")}
                        {:doc doc})))

         (defn room-data [doc]
           (let [pulled (fu/pull-doc doc)
                 message-coll (f/coll doc "messages")
                 members-doc (get pulled "members")]
             (with-meta {:id       (f/id doc)
                         :messages (with-meta (mapv message-data (f/docs message-coll))
                                              {:coll message-coll})
                         :members  (with-meta (mapv user-data members-doc)
                                              {:doc members-doc})}
                        {:doc doc})))

         (defn user-doc [email]
           (-> (f/coll db "users")
               (f/doc email)))

         (defn room-doc [room-id]
           (-> (f/coll db "rooms")
               (f/doc room-id)))

         (defn room-ids []
           (->> (f/coll db "rooms")
                (f/docs)
                (map f/id)
                (sort)))

         (def temp1 (atom nil))

         (watch-coll! "rooms/room1/messages"
                      (fn [x]
                        (reset! temp1 x)
                        (println x)
                        #_(println (.docChanges x))))

         (.data (first @temp1))

         (defn start-room! [room-id]

           (watch-coll! (str "rooms/" room-id "/messages")
                        (fn [x & others?]
                          (println others?)
                          (reset! temp1
                                  (mapv (fn [[id message]]
                                          {:id        id
                                           :content   (get message "content")
                                           :from      (user-data (get message "from"))
                                           :timestamp (get message "timestamp")})
                                        x)))))

         #_(mapv message-data (vals @temp1))

         #_(f/add-listener (f/coll db "rooms/room1/messages")
                           (fn [x y]
                             (reset! temp1 x)))

         (do @temp1)

         (defn all-rooms []
           (->> (f/coll db "rooms")
                (f/docs)
                (mapv room-data)))

         (defn user-rooms [user]
           (filter (fn [{:keys [members]}] (contains? (set members) user))
                   (all-rooms)))

         (defn other-rooms [user]
           (remove (fn [{:keys [members]}] (contains? (set members) user))
                   (all-rooms)))

         (comment
          (map :id (user-rooms (:user @*state))))

         (comment

          (-> (f/coll db "rooms")
              (f/filter-contains-any "members" [(-> (:user @*state) meta :doc)])
              (f/pull-query)
              (vals)
              #_(->> (mapv room-data)))

          (defn send-message! [state]
            ())

          (f/pull-doc (f/doc db "users/pbaille"))

          (room-data (room-doc "room1"))

          (mapv user-data
                (get (f/pull (f/doc db "room1"))
                     "members"))

          (f/id (f/coll (f/doc db "rooms/room1") "messages"))

          (fire/pull (f/coll db "rooms/room1/messages"))

          (f/pull-docs (f/docs (f/coll db "rooms/room1/messages")))

          (f/pull-doc (first (f/docs (f/coll db "rooms/room1/messages")))))

         )