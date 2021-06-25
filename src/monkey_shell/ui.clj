(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as comps]
            [clojure.walk :as walk]
            [monkey-shell.style.terminal :as term-style]
            [clojure.string :as str]
            [cljfx.css :as css]
            [monkey-shell.data :as data]
            [monkey-shell.state :as state]
            [monkey-shell.shell :as shell]))

(defn pending-cmd
  "Pending command component"
  [state {:as cmd :keys [cmd-args]}]
  (comps/hbox [(when (data/host-session? state (:focused-session state))
                 (comps/squared-btn {:pref-width 50 :text "▶︎" :style-class "app-term-btn"}
                                    {:event/type :session.pending.exec-cmd
                                     :cmd cmd}))
               {:fx/type :label
                :style-class "app-pending"
                :text (str/join " " cmd-args)}]))

(defn pending-cmds
  "Pending cmd list"
  [state]
  (when (= true (get-in state [:ui :pending-cmds-showing]))
    (comps/vbox (mapv (partial pending-cmd state)
                      (:pending (data/focused-session state))))))

(defn end-popup
  "Purpose: confirm or cancel btns"
  [confirm-event-keyword]
  (comps/hbox [(comps/squared-btn {:text "Confirm"} confirm-event-keyword)
               (comps/squared-btn {:text "Cancel"} :ui.popup.hide)]))

(defn error-popup []
  {:fx/type :label
   :text "Error, no content is available"})

(defn new-session-popup []
  (comps/vbox [{:fx/type :text-field
                :prompt-text "Type your session name here"
                :on-text-changed {:event/type :ui.session.set-new-id}}
               (end-popup :ui.popup.confirm-new-session)]))

(defn terminal-settings-popup [state]
  (comps/vbox [(comps/radio-group (data/members->true state))
               (end-popup :ui.popup.confirm-new-session)]))

(defn dynamic-popup [state]
  (comps/window (get-in state [:ui :popup :props])
                (get-in state [:ui :popup :content])))

(defn cmds->terminal-string [cmds]
  (reduce (fn [s {:as cmd :keys [text out pwd user]}]
            (str s "\n" (shell/prompt-string user pwd)
                 text
                 "\n\n" out))
          ""
          cmds))

(defn terminal
  "Terminal component"
  [state]
  (let [session (data/focused-session state)]
    (println "session env: " (:env session))
    (comps/hbox
      {:padding 0
       :style-class "app-code"
       :pref-width 800
       :pref-height 400
       :v-box/vgrow :always
       :spacing 0}

      [{:fx/type :scroll-pane
        :vvalue 1.0
        :style-class "app-code"
        :h-box/hgrow :always
        :content {:fx/type :v-box

                  :children [{:fx/type :label
                              :style-class "app-code"
                              :text (cmds->terminal-string (take-last 10 (:history session)))}
                             {:fx/type :h-box
                              :children [{:fx/type :label
                                          :style-class "app-code"
                                          :text (shell/prompt-string (get-in session [:env :USER])
                                                                     (get-in session [:env :PWD]))}
                                         {:fx/type :h-box :children [{:fx/type :text-field
                                                                      :style-class "app-text-field"
                                                                      :prompt-text "_"
                                                                      :text (get-in state [:ui :session :input])
                                                                      :on-text-changed {:event/type :ui.session.set-input}}]}]}]}}

       (comps/vbox {:alignment :top-right :min-width 150}
                   [(comps/hbox {:alignment :top-right} [(comps/squared-btn {:pref-width 30
                                                                             :text "⚙"
                                                                             :style-class "app-term-btn"}
                                                                            :ui.popup.shell-settings)
                                                         (comps/squared-btn {:pref-width 50
                                                                             :text (str ">⎽(" (data/count-pending-cmds state) ")")
                                                                             :style-class "app-term-btn"}
                                                                            :ui.terminal.toggle-show-pending-cmds)])
                    (pending-cmds state)])])))


(defn session [state]
  {:fx/type :stage
   :showing true
   :width 1050
   :height 600
   :scene {:fx/type :scene
           :stylesheets [(::css/url (term-style/style))]
           :root {:fx/type :v-box
                  :on-key-pressed {:event/type :keypressed}
                  :children [{:fx/type :h-box
                              :v-box/vgrow :always
                              :children [(comps/vbox [(comps/squared-btn {:pref-width 30 :text "+"} :ui.popup.new-session)
                                                      (comps/sidebar :ui.sidebar.click
                                                                     (keys (walk/stringify-keys
                                                                             (get state :shell-sessions))))])
                                         {:fx/type :v-box
                                          :h-box/hgrow :always
                                          :children [(terminal state)]}]}]}}})

(defn root [state]
  (comps/many [(session state)
               (dynamic-popup state)]))