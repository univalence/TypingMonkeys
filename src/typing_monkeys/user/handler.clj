(ns typing-monkeys.user.handler
  (:require [typing-monkeys.base :refer [*state handler]]
            [typing-monkeys.user.db :as db]
            [typing-monkeys.utils.cljfx :as ufx]))

(defmethod handler :user.signed-in [{:keys [email]}]
  (println "signed-in " email)
  (let [user (db/get-user email)]
    (handler {:event/type :chat.init :user user})
    (handler {:event/type :text.init :user user})
    (swap! *state assoc :user user)))

(defmethod handler :user.assoc [{:keys [key fx/event]}]
  (let [value (case key :color (ufx/color->hex event) event)
        state (swap! *state assoc-in [:user key] value)]
    (db/set-user! (:user state))))