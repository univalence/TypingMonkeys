(ns chat.views)

(defn message [{:keys [content from]}]
  #_(println "render message" content from)
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
  #_(println "render room button")
  {:fx/type  :h-box
   :spacing  5
   :padding  5
   :children [{:fx/type    :button
               :text       room-id
               :pref-width 300
               :on-action  {:event/type :chat.event/swap-room
                            :room-id    room-id}}]})

(defn chat [{:as                        state
             user                       :user
             {:keys [room input rooms]} :chat}]
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
                                             :on-action  {:event/type :chat.event/logout}}

                                            {:fx/type       :scroll-pane
                                             :grid-pane/row 1
                                             :fit-to-width  true
                                             :content       {:fx/type  :v-box
                                                             :children (mapv room-btn rooms)}}

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
                                                                                :children (mapv message
                                                                                                (sort-by :timestamp (:messages room)))}} ;; print all messages

                                                                {:fx/type         :text-field
                                                                 :v-box/margin    5
                                                                 :text            input
                                                                 :prompt-text     "Write message and press ENTER"
                                                                 :on-text-changed {:event/type :chat.event/type}
                                                                 :on-key-pressed  {:event/type :chat.event/send}}]}]}}})

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
                                   :on-text-changed {:event/type :chat.event/type-email}
                                   }

                                  {:fx/type         :password-field
                                   :v-box/margin    5
                                   :text            password
                                   :prompt-text     "Password"
                                   :on-text-changed {:event/type :chat.event/type-password}
                                   }

                                  {:fx/type      :button
                                   :v-box/margin 5
                                   :text         "Authenticate"
                                   :on-action    {:event/type :chat.event/authenticate}}]}}})


(defn root [{:as state :keys [page]}]
  (case page
    :auth (login state)
    :chat (chat state)
    (login state)))
