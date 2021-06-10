(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as ui]
            [clojure.walk :as walk]
            [monkey-shell.style.terminal :as term-style]
            [clojure.string :as str]
            [cljfx.css :as css]))



(defn error-popup []
  {:fx/type :label
   :text    "Error, no content is available"})

(defn new-session-popup []
  (ui/vbox [(ui/text-entry :ui.session.set-new-id :new-session "Add session")
            (ui/end-popup :ui.popup.confirm-new-session)]))

(defn terminal-settings-popup [state]
  (ui/vbox [(ui/radio-group (data/members->true state))
            (ui/end-popup :ui.popup.confirm-new-session)]))

(defn dynamic-popup [state]
  (println "dynpop"
           (ui/window (get-in state [:ui :popup :props])
                      (get-in state [:ui :popup :content])))
  (ui/window (get-in state [:ui :popup :props])
             (get-in state [:ui :popup :content])))


(defn text-thread
  "DEPRECATED:Chronologically ordered text.
  ATM it only prints the last cmd stdout,
  TODO print full history "
  [state]
  {:fx/type      :scroll-pane
   :pref-width   400
   :pref-height  400
   :v-box/vgrow  :always
   :fit-to-width true
   :content      {:fx/type  :v-box
                  :children [{:fx/type :text
                              :text    (-> (get-in state [:session :history])
                                           last :out str)}]}})

(defn terminal
  "Terminal component"
  [state]
  {:fx/type      :scroll-pane
   :style-class  "app-code"
   :pref-width   800
   :pref-height  400
   :v-box/vgrow  :always
   :fit-to-width true
   :content      (ui/hbox
                   {:padding 0
                    :spacing 0}
                   [{:fx/type     :v-box
                     :h-box/hgrow :always
                     :children    [{:fx/type     :label
                                    :style-class "app-code"
                                    :text        (str "<NAME>:<DIR>$ "
                                                      (str/join " " (-> (data/focused-session state)
                                                                        :history
                                                                        last
                                                                        :cmd-args))

                                                      "\n\n"

                                                      (-> (data/focused-session state)
                                                          :history last :out str))}

                                   {:fx/type  :h-box
                                    :children [{:fx/type     :label
                                                :style-class "app-code"
                                                :text        "<NAME>:<DIR>$"}
                                               {:fx/type  :h-box

                                                :children [{:fx/type         :text-field
                                                            :style-class     "app-text-field"
                                                            :prompt-text     "_"
                                                            :text            (get-in state [:ui :session :input])
                                                            :on-text-changed {:event/type :ui.session.set-input}}]}]}]}

                    (ui/squared-btn {:pref-width 30} "⚙" :ui.popup.shell-settings)])})


(defn session [state]
  {:fx/type :stage
   :showing true
   :width   1050
   :height  600
   :scene   {:fx/type     :scene
             :stylesheets [(::css/url (term-style/style))]
             :root        {:fx/type        :v-box
                           :on-key-pressed {:event/type :keypressed}
                           :children       [{:fx/type  :h-box
                                             :children [(ui/vbox [(ui/squared-btn {:pref-width 30} "+" :ui.popup.new-session)
                                                                  (ui/sidebar :ui.sidebar.click
                                                                              (keys (walk/stringify-keys
                                                                                      (get state :shell-sessions))))])
                                                        {:fx/type  :v-box
                                                         :children [(terminal state)]}]}]}}})

(defn test-window [state]
  (ui/window (get-in state [:ui :popup :props]) (ui/squared-btn "text" :ui.session.settings.close)))

(defn test-componenet []
  {:fx/type :label, :text "coucou"}
  )

(defn root [state]
  (ui/many [(session state)
            #_(test-componenet)
            #_(test-window state)
            (dynamic-popup state)]))