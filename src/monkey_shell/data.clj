(ns monkey-shell.data)

(defn new-session
  [id user]
  {:id      id
   :host    user
   :members [user]
   :history []})

(defn remove-pending-cmd
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

(defn with-new-session [state & [session-id]]
  (let [session-id (or session-id (str (gensym "shell_")))]
    (-> state
        (assoc :focused-session session-id)
        (assoc-in [:shell-sessions session-id]
                  (new-session session-id (:user state))))))

(defn with-focus [{:as state :keys [shell-sessions]} & [id]]
  (if-let [[session-id _]
           (or (find shell-sessions id)
               (first shell-sessions))]
    (assoc state :focused-session session-id)
    (with-new-session state)))

(defn host-session? [state session-id]
  (= (get-in state [:user :db/id])
     (get-in state [:shell-sessions (keyword session-id) :host :db/id])))

(defn count-pending-cmds [state]
  (count (get-in state [:shell-sessions (get state :focused-session) :pending])))