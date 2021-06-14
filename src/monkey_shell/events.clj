(ns monkey-shell.events
  (:require [monkey-shell.db :as db :refer [db]]
            [monkey-shell.state :as state :refer [*state]]
            [monkey-shell.shell :as shell]
            [clojure.string :as str]
            [monkey-shell.ui :as ui]
            [monkey-shell.data :as data]
            [typing-monkeys.utils.misc :as utils])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(declare handler)

(defn swap-session! [id f & args]
  (let [session (state/get [:shell-sessions (keyword id)])
        next-session (apply f session args)]
    (state/swap!_ (assoc-in _ [:shell-sessions id] next-session))
    (db/sync-session! (assoc next-session :id (name id)))))

(defmacro swap-session!_ [id & forms]
  `(swap-session! ~id (fn [session#]
                        (as-> session# ~'_ ~@forms))))

(defn init!
  ""
  [user-id]
  (let [sessions
        (db/fetch-user-sessions user-id)]

    (db/watch! sessions
               (fn [sessions-data]
                 (state/swap! assoc :shell-sessions sessions-data)))

    (state/swap!_
      (assoc _ :ui {:session {:settings {}}
                    :sidebar {}
                    :popup   {:content (ui/error-popup)
                              :props   {:showing false}}}

               :user (db/fetch-user user-id)
               :shell-sessions (db/pull-walk sessions))
      (state/with-focus _))))

(defn sync-session! []
  (db/sync-session! (state/get :session)))



(defn execute!
  ([] (let [state (state/get)
            cmd-args (str/split (get-in state [:ui :session :input]) #" ")]
        (execute! {:cmd-args cmd-args})))

  ([{:as cmd :keys [cmd-args]}]
   (let [state (state/get)
         session-id (keyword (:focused-session state))
         cmd (assoc cmd :from (get-in state [:user :id]) :id (utils/uuid) :out "")]

     (if (state/host-session? state session-id)
       (do (swap-session!_ session-id
                           (assoc _ :running true)
                           (update _ :history conj cmd))

           (shell/execute cmd-args
                          (fn [ret]
                            (swap-session!_ session-id
                                            (update _ :history
                                                    #(conj (pop %)
                                                           (assoc cmd :out (str (:out (last %)) ret))))))
                          (fn []
                            (swap-session!_ session-id (dissoc _ :running)))))

       (swap-session!_ session-id
                       (update _ :pending
                               (fnil conj [])
                               cmd))))))

(defn new-session! []
  (state/swap! state/with-new-session
               (keyword (state/get [:ui :session :new-id])))
  (sync-session!))

(defn add-session-member! []
  (state/swap! state/with-new-session
               (state/get [:ui :session :settings :new-member-id]))
  (sync-session!))

(defn focus-session! [session-id]
  (let [state (state/get)
        session (data/focused-session state)]
    #_(when (seq (:pending session))
        (handler {:event/type :ui.popup.set-content
                  :content    (ui/command-confirmation-popup (:pending session))}))
    (state/swap! state/with-focus session-id)))

(defn handler
  [event]
  (println "handling: " (:event/type event) (get event :click-payload))

  (case (:event/type event)

    :session.pending.exec-cmd
    (do
      (execute! (:cmd event))
      (state/swap! data/remove-pending-cmd (get-in event [:cmd :id])))


    :execute (do (execute!)
                 (handler {:event/type :ui.session.clear-input}))


    :new-session (new-session!)

    :session.add-member (add-session-member!)

    :session.focus (focus-session! (get event :session-id))

    :keypressed (condp = (.getCode ^KeyEvent (:fx/event event))
                  KeyCode/ENTER (handler {:event/type :execute})
                  nil)

    :ui.session.set-input
    (state/put! [:ui :session :input] (get event :fx/event))

    :ui.session.clear-input
    (state/put! [:ui :session :input] nil)

    :ui.sidebar.click
    (handler {:event/type :session.focus
              :session-id (keyword (get event :click-payload))})

    :ui.session.settings.open
    (state/put! [:ui :session :settings :window :showing] true)
    :ui.session.settings.close
    (state/put! [:ui :session :settings :window :showing] false)

    :ui.session.set-new-id
    (state/put! [:ui :session :new-id] (get event :fx/event))

    :ui.session.settings.set-new-member-id
    (state/put! [:ui :shell :settings :new-member-id] (get event :fx/event))

    :ui.popup.new-session
    (do
      (handler {:event/type :ui.popup.set-content
                :content    (ui/new-session-popup)})
      (handler {:event/type :ui.popup.show}))

    :ui.popup.confirm-new-session
    (do
      (handler {:event/type :ui.popup.hide})
      (handler {:event/type :new-session}))

    :ui.popup.shell-settings
    (do
      (handler {:event/type :ui.popup.set-content
                :content    (ui/terminal-settings-popup (state/get))})
      (handler {:event/type :ui.popup.show}))

    :ui.popup.show
    (state/put! [:ui :popup :props :showing] true)

    :ui.popup.hide
    (state/put! [:ui :popup :props :showing] false)

    :ui.popup.set-content
    (state/put! [:ui :popup :content] (:content event))))