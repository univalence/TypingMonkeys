(ns chat.core
  (:require [cljfx.api :as fx]
            [clj-http.client :as http]
            #_[conatus.db.firebase :as fire]
            [firestore-clj.core :as f])
  (:import [javafx.scene.input KeyCode KeyEvent]))

(do :help
    (defn pp [& xs]
      (mapv clojure.pprint/pprint xs) (last xs))

    (require '[clojure.string :as str]))

(do :auth

    (defn sign-in [email password ok ko]
      ;; sign in
      (http/request
       {:async        true
        :content-type :json
        :method       :post
        :url          "https://identitytoolkit.googleapis.com/v1/accounts:signInWithPassword?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
        :form-params  {:email email :password password :returnSecureToken true}}
       ok
       ko))

    #_(sign-in "pierrebaille@gmail.com" "password"
               (fn [x] (pp :ok x))
               (fn [x] (pp :ko x)))

    ;; sign up
    (defn sign-up [email password ok ko]
      (http/request
       {:async        true
        :content-type :json
        :method       :post
        :url          "https://identitytoolkit.googleapis.com/v1/accounts:signUp?key=AIzaSyDHvzDmWh7QIGx8UGLKHMdPH7JAWzNghus"
        :form-params  {:email email :password password :returnSecureToken true}}
       ok
       ko))

    #_(sign-in "pierrebaille@gmail.com" "password"
               (fn [x] (pp :ok x))
               (fn [x] (pp :ko x)))
    )

(do :firestore

    (require '[manifold.stream :as st])
    (defonce db (f/client-with-creds
                   "data/conatus-ef5f3-firebase-adminsdk-mowye-1aaf077bc3.json"))

    (defn watch-coll! [kw on-change]
      (st/consume
       on-change
       (f/->stream (f/coll db (name kw)))))

    (defn user-data [doc]
      (let [user (f/pull-doc doc)]
        (with-meta {:id     (f/id doc)
                    :pseudo (get user "pseudo")}
                   {:doc doc})))

    (defn message-data [doc]
      (let [message (f/pull-doc doc)]
        (with-meta {:id        (f/id doc)
                    :content   (get message "content")
                    :from      (user-data (get message "from"))
                    :timestamp (get message "timestamp")}
                   {:doc doc})))

    (defn room-data [doc]
      (with-meta {:id       (f/id doc)
                  :messages (mapv message-data (f/docs (f/coll doc "messages")))
                  :members  (mapv user-data
                                  (get (f/pull doc) "members"))
                  #_(mapv user-data (f/docs (f/coll doc "members")))}
                 {:doc doc}))

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



    (comment

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

(do :state

    (def state0
      {:user nil
       :chat {:room  nil
              :input ""}
       :auth {:email   ""
              :passord ""}})

    (def *state
      (atom {:user nil
             :chat {:room  nil
                    :input ""}
             :auth {:title       "Authentication"
                    :email "pierrebaille@gmail.com"
                    :password  "password"}}))

    (defn clear-input! []
      (swap! *state assoc :input ""))

    (defn set-user! [email]
      (when-let [user (user-doc email)]
        (swap! *state assoc :user (user-data user))))

    (defn set-room! [room-id]
      (when-let [room (room-doc room-id)]
        (swap! *state assoc :room (room-data room))))

    (defn set-page! [page-id]
      (swap! *state assoc :page page-id))

    (defn send-message! []
      (let [{:keys [user room input]} @*state]
        (clear-input!)
        (println "sending " input)
        )))

(do :views
    (defn message-component [{:keys [content from]}]
      #_(println "render message" content from)
      (println {:fx/type  :h-box
                :spacing  5
                :padding  5
                :children [{:fx/type :label
                            :style   {:-fx-text-fill :black}
                            :text    (str (:pseudo from) ": ")}
                           {:fx/type :label
                            :style   {:-fx-text-fill :black}
                            :text    content}]})
      {:fx/type  :h-box
       :spacing  5
       :padding  5
       :children [{:fx/type :label
                   :style   {:-fx-text-fill :black}
                   :text    (str (:pseudo from) ": ")}
                  {:fx/type :label
                   :style   {:-fx-text-fill :black}
                   :text    content}]})

    (defn room-btn [room-id]
      (println "render room button")
      {:fx/type  :h-box
       :spacing  5
       :padding  5
       :children [{:fx/type    :button
                   :text       room-id
                   :pref-width 300
                   :on-action  {:event/type ::swap-room
                                :room-id    room-id}}]})

    (defn chat [{room  :room
                 user  :user
                 input :input
                 :as   state}]
      #_(println "render chat ")
      #_(println (user-data user))
      #_(println (room-data room))
      {:fx/type :stage
       :width   960
       :height  400
       :showing true
       :scene   {:fx/type :scene
                 :root    {:fx/type            :grid-pane
                           :padding            10
                           :hgap               10
                           :column-constraints [{:fx/type       :column-constraints
                                                 :percent-width 20}
                                                {:fx/type       :column-constraints
                                                 :percent-width 80}]
                           :row-constraints    [{:fx/type        :row-constraints
                                                 :percent-height 10}
                                                {:fx/type        :row-constraints
                                                 :percent-height 90}]
                           :children           [{:fx/type          :label
                                                 :pref-width       200
                                                 :grid-pane/column 1
                                                 :text             (str "Current user: " (:pseudo user))}

                                                {:fx/type    :button
                                                 :text       "logout"
                                                 :pref-width 100
                                                 :on-action  {:event/type ::logout}}

                                                {:fx/type       :scroll-pane
                                                 :grid-pane/row 1
                                                 :fit-to-width  true
                                                 :content       {:fx/type  :v-box
                                                                 :children (mapv room-btn (room-ids))}}

                                                {:fx/type          :v-box
                                                 :grid-pane/column 1
                                                 :grid-pane/row    1
                                                 :pref-width       300
                                                 :pref-height      400
                                                 :children         [{:fx/type :label
                                                                     :text    (:id room)}

                                                                    {:fx/type      :scroll-pane
                                                                     :pref-height  800
                                                                     :fit-to-width true
                                                                     :content      {:fx/type  :v-box
                                                                                    :children (mapv message-component
                                                                                                    (:messages room))}} ;; print all messages

                                                                    {:fx/type         :text-field
                                                                     :v-box/margin    5
                                                                     :text            input
                                                                     :prompt-text     "Write message and press ENTER"
                                                                     :on-text-changed {:event/type ::type}
                                                                     :on-key-pressed  {:event/type ::send}}]}]}}})

    (defn login [{{:keys [email password]} :auth}]
      {:fx/type :stage
       :showing true
       :width   200
       :height  200
       :title   "Authentication"
       :scene   {:fx/type :scene
                 :root    {:fx/type  :v-box
                           :padding  10
                           :children [{:fx/type         :text-field
                                       :v-box/margin    5
                                       :text            email
                                       :prompt-text     "Email"
                                       :on-text-changed {:event/type ::type-email}
                                       }

                                      {:fx/type         :password-field
                                       :v-box/margin    5
                                       :text            password
                                       :prompt-text     "Password"
                                       :on-text-changed {:event/type ::type-pass}
                                       }

                                      {:fx/type      :button
                                       :v-box/margin 5
                                       :text         "Authenticate"
                                       :on-action    {:event/type ::authenticate}}]}}})

    ;;router
    (defn root [{:as state :keys [page]}]
      (case page
        :auth (login state)
        :chat (chat state)
        (login state)))
    )

(do :events
    (defn map-event-handler [event]
      #_(println "event " event)
      (case (:event/type event)

        ::type (swap! *state assoc :input (:fx/event event))
        ::send (when (= KeyCode/ENTER (.getCode ^KeyEvent (:fx/event event)))
                 (send-message!))

        ::swap-room (set-room! (:room-id event))

        ::logout (reset! *state state0)

        ::type-pass (swap! *state assoc-in [:auth :password] (:fx/event event))
        ::type-email (swap! *state assoc-in [:auth :email] (:fx/event event))

        ::authenticate (let [{:keys [email password]} (:auth @*state)]
                         (sign-in email password
                                  (fn [_]
                                    (set-user! email)
                                    (set-room! (first (room-ids)))
                                    (set-page! :chat))
                                  (fn [e]
                                    (pp :error e)))))))

(do :render

    (def renderer
      (fx/create-renderer
       :middleware (fx/wrap-map-desc assoc :fx/type root)
       :opts {:fx.opt/map-event-handler map-event-handler}))

    (fx/mount-renderer *state renderer))


