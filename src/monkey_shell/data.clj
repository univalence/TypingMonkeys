(ns monkey-shell.data)

(defn new-session
  [id user]
  {:id      id
   :host    user
   :members [user]
   :history []})

(defn
  remove-pending-cmd
  ""
  [state cmd-id]
  (update-in state [:shell-sessions (:focused-session state) :pending]
             (fn [cmds]
               (vec (remove #(= cmd-id (:id %)) cmds)))))

(defn focused-session
  "TODO move to DATA"
  [state]
  (get-in state [:shell-sessions (:focused-session state)]))

(defn members->true [state]
  "TODO : MOVE TO \"DATA\" NAMESPACE
  FIXME : not working"
  (as-> (focused-session state) _
        (get _ :members)
        (map :db/id _)
        (zipmap _ (repeat true))))