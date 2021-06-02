(ns monkey-shell.containers.shell-opts
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.components.core :as ui]))

(def *state (atom {:user           {:id "bastien@univalence.io"}
                   :input          nil
                   :session        {:id "first-shell"}

                   :shell-sessions {:first-shell  {:history []
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
                      conj [member-id]))))

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

#_(defn handler [{:keys [id fx/event]}]
    (swap! *state assoc :module (or id (keyword event))))

#_(defn open-settings []
  (fx/on-fx-thread
    (fx/create-component
      {:fx/type fx/ext-many
       :desc    [(ui/window
                   (ui/vbox [(ui/radio-group (members->true))
                             (ui/text-entry :capture-new-member-text :add-member "Add member")
                             (ui/squared-btn "OK" :mock)]))]})))

(defn member-select [_]
  {:fx/type fx/ext-many
   :desc    [(ui/window
               (ui/vbox [(ui/radio-group (members->true))
                         (ui/text-entry :capture-new-member-text :add-member "Add member")
                         (ui/squared-btn "OK" :mock)]))]})

(declare handler)

(def settings-renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type member-select)
    :opts {:fx.opt/map-event-handler handler}))


(defn root [state] {:fx/type :stage
                    :showing true
                    :width   600
                    :height  600
                    :scene   {:fx/type :scene
                              :root    {:fx/type  :v-box
                                        :children [(ui/text-entry :capture-new-room-text :create-session "Add Session")
                                                   {:fx/type  :h-box
                                                    :children [(ui/sidebar :handle-sidebar-click
                                                                           (keys (walk/stringify-keys
                                                                                   (get state :shell-sessions))))
                                                               {:fx/type  :v-box
                                                                :children [(ui/text-thread state)
                                                                           (ui/text-entry :capture-text :execute)
                                                                           (ui/squared-btn (str (get-in @*state [:session :id]) "'s settings") :open-settings)]}]}]}}})

(defn handler
  "HANDLER"
  [event]
  #_(println (get event :session-id))
  (case (:event/type event)
    :capture-text (swap! *state assoc :input (get event :fx/event))
    :execute (do (execute! (get-in @*state [:session :id])))
    :handle-sidebar-click (swap! *state assoc-in [:session :id] (get event :click-payload))
    :capture-new-room-text (swap! *state assoc :room-to-create (get event :fx/event))
    :create-session (create-session! (get @*state :room-to-create))
    :open-settings (settings-renderer {})
    :capture-new-member-text (swap! *state assoc :member-to-add (get event :fx/event))
    :add-member (add-member (get @*state :member-to-add))
    :mock (println "TODO")))

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler handler}))

(println @*state)