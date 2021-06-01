(ns typing-monkeys.core
  (:require [cljfx.api :as fx]
            [typing-monkeys.base :refer [*state handler reset-state!]]
            [typing-monkeys.user.handler]
            [typing-monkeys.user.views :as user-views]
            [typing-monkeys.auth.handler]
            [typing-monkeys.auth.view :as auth-view]
            [typing-monkeys.text.handler]
            [typing-monkeys.text.views :as text-views]
            [typing-monkeys.chat.handler]
            [typing-monkeys.chat.view :as chat-view]
            [typing-monkeys.db :as db]
            [typing-monkeys.utils.firestore :as fi]
            [typing-monkeys.style.sheet :as stylesheet]))

(fi/overide-print-methods)

(def MODULES ["chat" "text" "user"])

(defmethod handler :module-switch
  [{:keys [id fx/event]}]
  (swap! *state assoc :module (or id (keyword event))))

(defn current-module
  [{:as state :keys [module chat text user]}]
  (case module
    :chat (chat-view/root2 chat)
    :text (text-views/root text)
    :user (user-views/editor user)))

(defn topbar [{:as state :keys [user module]}]

  {:fx/type   :h-box
   :alignment :top-right
   :padding   5
   :spacing   5
   :children  [{:fx/type          :combo-box
                :value            (get-in state [:auth :email])
                :items            ["logout"]
                :on-value-changed {:event/type :auth.logout}}
               {:fx/type     :region
                :h-box/hgrow :always}
               {:fx/type          :combo-box
                :value            (name module)
                :items            MODULES
                :on-value-changed {:event/type :module-switch}}]})

(defn home [state]
  {:fx/type :stage
   :showing true
   :title   "Typing Monkeys"
   :x       500
   :y       -1000
   :width   960
   :height  540
   :scene   {:fx/type        :scene
             :stylesheets    [(:cljfx.css/url stylesheet/main)]
             :root           (merge {:fx/type     :border-pane
                                     :style-class "root"
                                     :top         (topbar state)}
                                    (current-module state))}})

(defn root [{:as state :keys [user]}]
  (if user
    (home state)
    (auth-view/login (:auth state))))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
   :opts {:fx.opt/map-event-handler handler}))

(reset-state!)

(def mounted-render (fx/mount-renderer *state renderer))

(renderer)
