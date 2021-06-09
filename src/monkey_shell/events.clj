(ns monkey-shell.events
  (:require [monkey-shell.db :as db :refer [db]]
            [monkey-shell.state :as state :refer [*state]]
            [monkey-shell.shell :as shell]
            [clojure.string :as str]))

(defn swap-session! [id f]
  (let [session (state/get [:shell-sessions (keyword id)])
        next-session (f session)]
    (state/swap!_ (assoc-in _ [:shell-sessions id] next-session))
    (db/sync-session! (assoc next-session :id (name id)))))

(defmacro swap-session!_ [id & forms]
  `(swap-session! ~id (fn [session#]
                        (as-> session# ~'_ ~@forms))))

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

  (let [state (state/get)
        session-id (keyword (get-in state [:session :id]))
        cmd-args (str/split (get-in state [:ui :session :input]) #" ")]

    (if (state/host-session? state session-id)
      (shell/execute cmd-args
                     (fn [ret]
                       (swap-session!_ session-id
                                       (assoc _ :running true)
                                       (update _ :history #(conj (pop %)
                                                                 {:cmd-args cmd-args :out ret}))))
                     (fn []
                       (swap-session!_ session-id (dissoc _ :running))))

      (swap-session!_ session-id
                      (update _ :pending
                              (fnil conj [])
                              {:cmd-args cmd-args
                               :from (get-in state [:user :id])})))))

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

    :ui.session.settings.open
    (state/put! [:ui :session :settings :window :showing] true)
    :ui.session.settings.close
    (state/put! [:ui :session :settings :window :showing] false)

    :ui.session.set-new-id
    (state/put! [:ui :session :new-id] (get event :fx/event))

    :ui.session.settings.set-new-member-id
    (state/put! [:ui :shell :settings :new-member-id] (get event :fx/event))
    ))