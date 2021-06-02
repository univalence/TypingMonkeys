(ns monkey-shell.core
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [firestore-clj.core :as f]
            [clojure.walk :as walk]
            [manifold.stream :as st]
            [typing-monkeys.utils.firestore :as fu]
            [clojure.java.shell :as shell])
  (:import (java.util HashMap ArrayList)))

(defonce db
  (f/client-with-creds "data/unsafe.json"))

(def *state (atom {}))

(defn fetch-user-sessions
  [user-id]
  (-> (f/coll db "shell-sessions")
      (f/filter-contains "members" (f/doc db (str "users/" user-id)))))

(defn pull-walk [x]
  (cond
    (or (fu/ref? x) (fu/query? x)) (pull-walk (f/pull x))
    (map? x) (into {} (map (fn [[k v]] [(keyword k) (pull-walk v)]) x))
    (vector? x) (mapv pull-walk x)
    (instance? HashMap x) (pull-walk (into {} x))
    (instance? ArrayList x) (pull-walk (into [] x))
    :else x))

(defn fetch-user
  [user-id]
  (-> (f/doc db (str "users/" user-id))
      (f/pull-doc)
      (walk/keywordize-keys)
      (assoc :id user-id)))

(defn listen!
  [query k]
  (st/consume (fn [x]
                (swap! *state assoc k (pull-walk x)))
              (f/->stream query)))

(defn init!
  [user-id]
  (let [sessions (fetch-user-sessions user-id)
        sessions-data (pull-walk sessions)
        [session-id session-data] (first sessions-data)]
    (swap! *state assoc
           :user (fetch-user user-id)
           :input ""
           :session (assoc session-data :id (name session-id))
           :shell-sessions sessions-data)
    (listen! sessions :shell-sessions)))

(init! "pierrebaille@gmail.com")

(deref *state)

(defn sync-session!
  []
  (let [session (get @*state :session)]
    (-> (f/coll db "shell-sessions")
        (f/doc (:id session))
        (f/set! (walk/stringify-keys session)))))

(defn execute! []
  (swap! *state
         (fn [state]
           (let [cmd-args (str/split (:input state) #" ")
                 result (apply shell/sh cmd-args)]
             (update-in state [:session :history]
                        conj {:cmd-args cmd-args
                              :result result})))))

(defn handler [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn map-event-handler [event]
  (case (:event/type event)
    :capture-text (swap! *state assoc :input (get event :fx/event))
    :execute (do (execute!) (sync-session!))))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width 600
                    :height 600
                    :y -1000
                    :scene {:fx/type :scene
                            :root {:fx/type :v-box
                                   :children [{:fx/type :text
                                               :text (-> (get-in state [:session :history])
                                                         last :result :out str)}
                                              {:fx/type :h-box
                                               :children [{:fx/type :text-field
                                                           :on-text-changed {:event/type :capture-text}}
                                                          {:fx/type :button
                                                           :text "ENTER"
                                                           :pref-width 100
                                                           :on-action {:event/type :execute}}]}]}}})

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

(deref *state)

