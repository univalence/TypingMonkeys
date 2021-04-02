(ns typing-monkeys.text.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.base :refer [client-id]]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.utils.walk :as walk]
            [typing-monkeys.text.crdt :as d]))

(defn user_ref->data [ref]
  (let [user-ref (f/pull-doc ref)]
    (db/with-ref ref
                 {:id     (f/id ref)
                  :pseudo (get user-ref "pseudo")})))

(defn get-user [email]
  (f/doc db (str "users/" email)))


(defn text-ref->data [ref]
  (let [text (f/pull-doc ref)]
    (db/with-ref ref
                 {:id           (f/id ref)
                  :last-client  (get text "last-client")
                  :timestamp    (get text "timestamp")
                  :tree         (read-string (get text "tree"))
                  :members      (mapv walk/keywordize-keys (get text "members"))})))

(defn text-ref [id]
  (f/doc db (str "crdt-strings/" id)))

(defn get-text [id]
  (text-ref->data (text-ref id)))

(defn get-text-ids []
  (->> (f/coll db "crdt-strings")
       (f/docs)
       (map f/id)
       (sort)))

(defn get-texts []
  (mapv get-text (get-text-ids)))

(defn get-user-text-ids [user-ref]
  (map :id (filter (fn [{:keys [members]}] (contains? (set (map :user members)) user-ref))
                   (get-texts))))


(defn sync-state!
  [{:as text :keys [user position timestamp local-changes]}]
  #_(println "sync-state " (db/data->ref tree) tree)
  (f/update! (db/data->ref text)
             (fn [text]
               (let [timestamp (max timestamp (get text "timestamp"))
                     sorted-members (sort-by #(get % "last-sync") (get text "members"))
                     compressable? (= user (get (first sorted-members) "user"))
                     compression-limit (some-> sorted-members second (get "last-sync"))]
                 (println "compressable? " compressable?
                          "compression-limit " compression-limit)
                 (-> text
                     (assoc "last-client" client-id "timestamp" timestamp)
                     (update "tree" (fn [tree] (let [new-tree (reduce d/insert (read-string tree) local-changes)]
                                                 (str (if compressable? (d/compress new-tree compression-limit) new-tree)))))
                     (update "members" (fn [members]
                                         (mapv (fn [member]
                                                 (if (= user (get member "user"))
                                                   (assoc (into {} member) "position" position "last-sync" timestamp)
                                                   member))
                                               members))))))))

(defn watch-text! [id on-change]
  (st/consume (fn [x]
                (on-change (text-ref->data (f/ref x))))
              (f/->stream (text-ref id)
                          {:plain-fn identity})))



(comment

 (f/update! (f/doc db "scratch/foobar")
            (fn [x] {"foo" "boz"}))

 (st/consume (fn [x]
               (println "tchanged: " x))
             (f/->stream (f/doc db "scratch/foobar")
                         {:plain-fn identity}))

 (watch-text! "first" (partial pp "changed"))
 (f/->stream (text-ref "first"))

 (text-ref->data (f/doc db "crdt-strings/first")))

(defn reset-text [id]
  (let [kw->str (partial walk/postwalk-replace #(when (keyword? %) (name %)))
        text-ref (f/doc db (str "crdt-strings/" id))]
    (f/delete! text-ref)
    (f/create! text-ref
               (kw->str {:timestamp 1
                         :tree    (str d/zero)
                         :members [{:user     (f/doc db "users/pierrebaille@gmail.com")
                                    :id       1
                                    :color    "lightskyblue"
                                    :position [0 0]}
                                   {:user     (f/doc db "users/francois@univalence.io")
                                    :id       2
                                    :color    "tomato"
                                    :position [0 0]}]}))))

#_(reset-text "first")

(comment :first-tree-init

         )