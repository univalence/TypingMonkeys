(ns typing-monkeys.core2
  (:require [cljfx.api :as fx]
            [typing-monkeys.base2 :refer [*state handler]]
            [typing-monkeys.auth2.events]
            [typing-monkeys.auth2.view :as auth-view]
            [typing-monkeys.text2.handler]
            [typing-monkeys.text2.views :as text-views]
            [typing-monkeys.chat.handler :as chat-handler]
            [typing-monkeys.chat.view :as chat-view]
            [typing-monkeys.db :as db]
            [typing-monkeys.utils.firestore :as fi]))

(fi/overide-print-methods)

(defmethod handler :signed-in [{:keys [email]}]
  (println "signed-in " email)
  (let [user (db/get-user email)]
    (handler {:event/type :text.init :user user})
    (handler {:event/type :chat.init :user user})
    (swap! *state assoc :user user)))

(defmethod handler :tab-switch [{:keys [id]}]
  (swap! *state assoc :tab id))

(defn tab [kw content]
  {:fx/type          :tab
   :text             (name kw)
   :closable         false
   :on-selection-changed {:event/type :tab-switch :id kw}
   :content          content})

(def tabs
  {:chat chat-view/root
   :text text-views/root})

(defn home [state]
  {:fx/type :stage
   :showing true
   :title   "Typing Monkeys"
   :x       500
   :y       -1000
   :width   960
   :height  540
   :scene   {:fx/type        :scene
             :on-key-pressed {:event/type :text.keypressed}
             :root           {:fx/type         :tab-pane
                              :tabs            (mapv (fn [[name view]] (tab name (view (get state name))))
                                                     tabs)}}})

(defn root [{:as state :keys [user]}]
  (if user
    (home state)
    (auth-view/login (:auth state))))

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc assoc :fx/type root)
   :opts {:fx.opt/map-event-handler handler}))

(def mounted-render (fx/mount-renderer *state renderer))

#_(renderer)