(ns monkey-shell.data)

(defn new-session
  [id user]
  {:id id
   :host user
   :members [user]
   :history []})