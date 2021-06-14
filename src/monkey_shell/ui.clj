(ns monkey-shell.ui
  (:require [monkey-shell.components.core :as comps]
            [clojure.walk :as walk]
            [monkey-shell.style.terminal :as term-style]
            [clojure.string :as str]
            [cljfx.css :as css]
            [monkey-shell.data :as data]))

(defn pending-cmd
  "Pending command component"
  [{:as cmd :keys [cmd-args]}]
  (comps/hbox [{:fx/type :label
                :text    (str/join " " cmd-args)}
               (comps/squared-btn {:text "EXEC"} {:event/type :session.pending.exec-cmd
                                                  :cmd        cmd})]))

(defn pending-cmds
  "Pending cmd list"
  [state]
  (comps/vbox (mapv pending-cmd
                    (:pending (data/focused-session state)))))

(defn end-popup
  "Purpose: confirm or cancel btns"
  [confirm-event-keyword]
  (comps/hbox [(comps/squared-btn {:text "Confirm"} confirm-event-keyword)
               (comps/squared-btn {:text "Cancel"} :ui.popup.hide)]))

(defn error-popup []
  {:fx/type :label
   :text    "Error, no content is available"})

(defn new-session-popup []
  (comps/vbox [(comps/text-entry :ui.session.set-new-id :new-session "Add session")
               (end-popup :ui.popup.confirm-new-session)]))

(defn terminal-settings-popup [state]
  (comps/vbox [(comps/radio-group (data/members->true state))
               (end-popup :ui.popup.confirm-new-session)]))

(defn dynamic-popup [state]
  (println "dynpop"
           (comps/window (get-in state [:ui :popup :props])
                         (get-in state [:ui :popup :content])))
  (comps/window (get-in state [:ui :popup :props])
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
   :content      (comps/hbox
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
                                                      "\n\n" (-> (data/focused-session state)
                                                                 :history last :out str))}
                                   {:fx/type  :h-box
                                    :children [{:fx/type     :label
                                                :style-class "app-code"
                                                :text        "<NAME>:<DIR>$"}
                                               {:fx/type :h-box :children [{:fx/type         :text-field
                                                                            :style-class     "app-text-field"
                                                                            :prompt-text     "_"
                                                                            :text            (get-in state [:ui :session :input])
                                                                            :on-text-changed {:event/type :ui.session.set-input}}]}]}]}
                    (comps/vbox [(comps/squared-btn {:pref-width 30 :text "⚙"} :ui.popup.shell-settings)
                                 (pending-cmds state)])])})

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
                                             :children [(comps/vbox [(comps/squared-btn {:pref-width 30 :text "+"} :ui.popup.new-session)
                                                                     (comps/sidebar :ui.sidebar.click
                                                                                    (keys (walk/stringify-keys
                                                                                            (get state :shell-sessions))))])
                                                        {:fx/type  :v-box
                                                         :children [(terminal state)]}]}]}}})

(defn root [state]
  (comps/many [(session state)
               (dynamic-popup state)]))