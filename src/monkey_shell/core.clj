(ns monkey-shell.core
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [firestore-clj.core :as f]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.components.core :as ui]))

(def *state (atom {:history       ()
                   :shell-session {:id "first-shell"}}))

(defonce db (f/client-with-creds "data/unsafe.json"))

(def shell-sessions
  (swap! *state (fn [state]
                  (update state :shell-session
                          merge
                          (-> (f/coll db "shell-sessions")
                              (f/doc (get-in state [:shell-session :id]))
                              (f/pull-doc)
                              (update "members" f/pull-docs)
                              (walk/keywordize-keys))))))

(defn sync-session! []
  (-> (f/coll db "shell-sessions")
      (f/doc (get-in @*state [:shell-session :id]))
      (f/assoc! "history" (vec (walk/stringify-keys (:history @*state))))))

(defn execute! []
  (swap! *state
         (fn [state]
           (let [cmd-args (str/split (:text state) #" ")
                 result (apply shell/sh cmd-args)]
             (update state :history conj {:cmd-args cmd-args
                                          :result   result})))))

(defn handler [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn map-event-handler [event]
  (case (:event/type event)
    :capture-text (swap! *state assoc :text (get event :fx/event))
    :execute (do (execute!) (sync-session!))
    ))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   600
                    :height  200
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :v-box
                                        :children [{:fx/type :text
                                                    :text    (-> (get state :history)
                                                                 first
                                                                 :result
                                                                 :out
                                                                 str)}
                                                   (ui/text-entry :capture-text :execute)]}}})



(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler map-event-handler}))

(get @*state :shell-session)

