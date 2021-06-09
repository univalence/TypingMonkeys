(ns monkey-shell.scratch.shell-opts
  (:require [cljfx.api :as fx]
            [cljfx.css :as css]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.components.core :as ui]
            [monkey-shell.style.terminal :as term-style]
            [clojure.string :as str]))

(def *state (atom {:settings-window {:showing false}

                   :user            {:id "bastien@univalence.io"}
                   :input           nil
                   :session         {:id "first-shell"}

                   :shell-sessions  {:first-shell  {:history []
                                                    :members ["bastien@univalence.io" "pierre@univalence.io"]}
                                     :second-shell {:history []
                                                    :members ["bastien@univalence.io" "francois@univalence.io"]}}}))

(defn members->true []
  (zipmap (get-in @*state
                  [:shell-sessions (keyword (get-in @*state [:session :id]))
                   :members]) (repeat true)))

(defn add-member [member-id]
  (swap! *state
         (fn [state]
           (update-in state
                      [:shell-sessions (keyword (get-in state [:session :id])) :members]
                      conj (str member-id)))))

(defn create-session! [session-name]
  (swap! *state
         (fn [state]
           (assoc-in state [:shell-sessions (keyword session-name)]
                     {:history []
                      :members []}))))

(defn execute!
  "TODO DOC"
  [shell-session]
  #_(println (get-in @*state [:histories (keyword shell-session)]))
  (swap! *state
         (fn [state]
           (let [cmd-args (str/split (:input state) #" ")
                 result (apply shell/sh cmd-args)]

             (update-in state
                        [:shell-sessions (keyword shell-session) :history]
                        conj {:cmd-args cmd-args
                              :result   result})))))

(defn member-select [state]
  (ui/window (:settings-window state)
             (ui/vbox [(ui/radio-group (members->true))
                       (ui/text-entry :capture-new-member-text :add-member "Add member")
                       (ui/squared-btn "OK" :close-settings)])))

(declare handler)

(def settings-renderer "fixme change name"
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type member-select)
    :opts {:fx.opt/map-event-handler handler}))

(type settings-renderer)
(defn terminal-with-opts [])

(defn parse-ansi
  "Purpose: reads string with ansi escape sequences for colors
  then pops out a colored output"
  [str color-config]
  (println "not implemented yet: parse-ansi"))

(defn terminal
  ""
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
                   [{:fx/type  :v-box
                     :h-box/hgrow :always
                     :children [{:fx/type     :label
                                 :style-class "app-code"
                                 :text        (str "<NAME>:<DIR>$ "
                                                   (str/join " " (-> (get-in state [:shell-sessions (keyword (get-in state [:session :id])) :history])
                                                                     first
                                                                     :cmd-args))

                                                   "\n\n"

                                                   (-> (get-in state [:shell-sessions (keyword (get-in state [:session :id])) :history])
                                                       first
                                                       :result
                                                       :out
                                                       str))}

                                {:fx/type  :h-box
                                 :children [{:fx/type     :label
                                             :style-class "app-code"
                                             :text        "<NAME>:<DIR>$"}
                                            {:fx/type  :h-box

                                             :children [{:fx/type         :text-field
                                                         :style-class     "app-text-field"
                                                         :prompt-text     "_"
                                                         :text            (:input state)
                                                         :on-text-changed {:event/type :capture-text}}
                                                        (ui/squared-btn "EXEC" :execute)]}]}]}

                    (ui/squared-btn {:pref-width 30} "⚙" :open-settings)])})

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   1050
                    :height  600
                    :scene   {:fx/type     :scene
                              :stylesheets [(::css/url (term-style/style))]
                              :root        {:fx/type  :v-box
                                            :children [(ui/text-entry :capture-new-session-text :create-session "Add Session")
                                                       {:fx/type  :h-box
                                                        :children [(ui/sidebar :handle-sidebar-click
                                                                               (keys (walk/stringify-keys
                                                                                       (get state :shell-sessions))))
                                                                   {:fx/type  :v-box
                                                                    :children [(terminal state)
                                                                               (ui/squared-btn (str (get-in @*state [:session :id]) "'s settings") :open-settings)]}]}]}}})

(defn many [state]
  (ui/many [(root state) (member-select state)]))

(defn handler
  "HANDLER"
  [event]
  #_(println (get event :session-id))
  (case (:event/type event)
    :capture-text (swap! *state assoc :input (get event :fx/event))
    :execute (do (execute! (get-in @*state [:session :id]))
                 (swap! *state assoc :input ""))
    :handle-sidebar-click (swap! *state assoc-in [:session :id] (get event :click-payload))
    :capture-new-session-text (swap! *state assoc :room-to-create (get event :fx/event))
    :create-session (create-session! (get @*state :room-to-create))
    :open-settings (swap! *state assoc-in [:settings-window :showing] true)
    :close-settings (swap! *state assoc-in [:settings-window :showing] false)
    :capture-new-member-text (swap! *state assoc :member-to-add (get event :fx/event))
    :add-member (add-member (get @*state :member-to-add))
    :mock (println "TODO")))

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type many)
    :opts {:fx.opt/map-event-handler handler}))

(println @*state)