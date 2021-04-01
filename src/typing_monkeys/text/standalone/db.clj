(ns typing-monkeys.text.db
  (:require [firestore-clj.core :as f]
            [typing-monkeys.db :as db :refer [db]]
            [typing-monkeys.utils.firestore :as fu]
            [manifold.stream :as st]
            [typing-monkeys.utils.misc :as u :refer [pp]]
            [typing-monkeys.utils.walk :as walk]
            [xp.data-laced-with-history :as d]))

(defn tree_syncable [t]
  (walk/postwalk-replace #(when (keyword? %) (name %))
                         t))

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
                  :last-updater (get text "last-updater")
                  :tree         (read-string (get text "tree"))
                  :members      (mapv walk/keywordize-keys (get text "members"))})))

(defn text-ref [id]
  (f/doc db (str "crdt-strings/" id)))

(defn get-text [id]
  (text-ref->data (text-ref id)))

(defn sync-state! [{:as text :keys [tree user position]} uuid]
  #_(println "sync-state " (db/data->ref tree) tree)
  (let [local-changes (-> text meta :local-changes)]
    (f/update! (db/data->ref text)
               (fn [x] (-> x
                           (assoc "last-updater" uuid)
                           (update "tree" (fn [tree] (str (reduce d/insert (read-string tree) local-changes))))
                           (update "members" (fn [members]
                                               (mapv (fn [member]
                                                       (if (= user (get member "user"))
                                                         (assoc (into {} member) "position" position)
                                                         member))
                                                     members))))))))

(defn watch-text [id on-change]
  (st/consume (fn [x]
                #_(pp "tree changed: " x (f/ref x))
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

 (watch-text "first" (partial pp "changed"))
 (f/->stream (text-ref "first"))

 (text-ref->data (f/doc db "crdt-strings/first")))

(defn reset-first-text []
  (let [kw->str (partial walk/postwalk-replace #(when (keyword? %) (name %)))]
    (f/delete! (f/doc db "crdt-strings/first"))
    (f/create! (f/doc db "crdt-strings/first")
               (kw->str {:tree    (str d/zero) #_(kw->str (reduce d/insert d/zero d/data))
                         :members [{:user     (f/doc db "users/pierrebaille@gmail.com")
                                    :id       1
                                    :color    "lightskyblue"
                                    :position [0 0]}
                                   {:user     (f/doc db "users/francois@univalence.io")
                                    :id       2
                                    :color    "tomato"
                                    :position [0 0]}]}))))
(comment :first-tree-init

         )