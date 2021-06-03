(ns monkey-shell.events
  (:require [monkey-shell.db :as db :refer [db]]
            [monkey-shell.state :as state :refer [*state]]
            [clojure.string :as str]
            [clojure.java.shell :as shell]))



(defn init!
  [user-id]
  (let [sessions
        (db/fetch-user-sessions user-id)]

    (db/watch! sessions
               (fn [sessions-data]
                 (state/swap!_ (assoc _ :shell-sessions sessions-data)
                               (state/with-focus _ (keyword (get-in _ [:session :id]))))))

    (state/swap!_
      (assoc _ :ui {:session {:settings {:window {:showing false}}}
                    :sidebar {}}
               :user (db/fetch-user user-id)
               :shell-sessions (db/pull-walk sessions))
      (state/with-focus _ first))))

(defn sync-session! []
  (db/sync-session! (state/get :session)))

(defn execute! []
  (state/swap!_
    (let [cmd-args (str/split (get-in _ [:ui :session :input]) #" ")
          result (apply shell/sh cmd-args)]
      (update-in _ [:session :history]
                 conj {:cmd-args cmd-args
                       :result result})))
  (sync-session!))

(defn new-session! []
  (state/swap! state/with-new-session
               (state/get [:ui :session :new-id]))
  (sync-session!))

(defn add-member! []
  (state/swap! state/with-new-session
               (state/get [:ui :session :settings :new-member-id]))
  (sync-session!))

(defn handler
  [event]
  (println "handling: " (:event/type event) (get event :click-payload))
  (case (:event/type event)

    :execute (execute!)
    :new-session (new-session!)
    :add-member (add-member!)

    :ui.session.set-input
    (state/put! [:ui :session :input] (get event :fx/event))

    :ui.sidebar.click
    (state/swap! state/with-focus (keyword (get event :click-payload)))

    ::ui.session.settings.open
    (state/put! [:ui :session :settings :window :showing] true)
    ::ui.session.settings.close
    (state/put! [:ui :session :settings :window :showing] false)

    :ui.session.set-new-id
    (state/put! [:ui :session :new-id] (get event :fx/event))

    :ui.session.settings.set-new-member-id
    (state/put! [:ui :shell :settings :new-member-id] (get event :fx/event))
    ))