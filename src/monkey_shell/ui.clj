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
  {:fx/type :label
   :text    "tba"})

(defn dynamic-popup [state]
  (ui/window (get-in state [:ui :popup :props])
             (get-in state [:ui :popup :content])))

(defn focused-session [state]
  (get-in state [:shell-sessions (:focused-session state)]))

(defn members->true [state]
  "TODO : MOVE TO \"DATA\" NAMESPACE"
  (as-> (focused-session state) _
        (get _ :members)
        (map :id _)
        (zipmap _ (repeat true))))

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
                                                      (str/join " " (-> (focused-session state)
                                                                        :history
                                                                        last
                                                                        :cmd-args))

                                                      "\n\n"

                                                      (-> (focused-session state)
                                                          :history last :out str))}

                                   {:fx/type  :h-box
                                    :children [{:fx/type     :label
                                                :style-class "app-code"
                                                :text        "<NAME>:<DIR>$"}
                                               {:fx/type  :h-box

                                                :children [{:fx/type         :text-field
                                                            :style-class     "app-text-field"
                                                            :prompt-text     "_"
                                                            :text            (:input state)
                                                            :on-text-changed {:event/type :ui.session.set-input}}]}]}]}

                    (ui/squared-btn {:pref-width 30} "⚙" :ui.session.settings.open)])})


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

(defn session-settings [state]
  (ui/window (get-in state [:ui :session :settings :window])
             (ui/vbox [(ui/radio-group (members->true state))
                       (ui/text-entry :ui.session.settings.set-new-id :add-member "Add member")
                       (ui/squared-btn "OK" :ui.session.settings.close)])))

(defn root [state]
  (ui/many [(session state)
            (dynamic-popup state)]))