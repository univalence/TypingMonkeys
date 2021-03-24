(ns xp.cljfx.barebone
  (:require [cljfx.api :as fx]
            [typing-monkeys.utils.misc :as u :refer [pp fk]]
            [typing-monkeys.utils.cljfx :refer [defc]]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.component :as component]))

(defc stage [title scene]
      :showing true
      :width 200
      :height 200
      :x 500
      :y -1000)

(defc scene [root])

(defc label [text])

(def *state (atom {:foo {:one {:bar :baz}}}))

(def *env
  (atom {:foo {:get  (fn [state desc] (get-in state [:foo (:id desc)]))
               :view (fk [data] (label (str data)))}}))

(defn root [_]
  (stage "root"
         (scene {:fx/type :foo :id :one})))

(do :dyn-env

    (defn wrap-dynamic-env-fn [lifecycle]
      (with-meta
       [::dynamic-env-fn lifecycle]
       {`lifecycle/create
        (fn [_ desc opts]
          (let [{:keys [get view]} (u/get @*env (:fx/type desc))
                data (get @*state desc)
                child-desc (view (assoc desc :data data))]
            (with-meta {:child-desc child-desc
                        :desc       desc
                        :data       data
                        :view-fn    view
                        :child      (lifecycle/create lifecycle child-desc opts)}
                       {`component/instance #(-> % :child component/instance)})))
        `lifecycle/advance
        (fn [_ {:as component :keys [child-desc view-fn data] old-desc :desc} desc opts]
          (if (= desc old-desc)
            (update component :child #(lifecycle/advance lifecycle % child-desc opts))
            (let [child-desc (view-fn (assoc desc :data data))]
              (-> (assoc component :child-desc child-desc :desc desc)
                  (update :child #(lifecycle/advance lifecycle % child-desc opts))))))
        `lifecycle/delete
        (fn [_ component opts]
          (lifecycle/delete lifecycle (:child component) opts))}))

    (def dynamic-env-fn->dynamic
      (wrap-dynamic-env-fn lifecycle/dynamic)))

(defn event-handler [event])

(defn type->lifecycle [type]
  (cond (keyword? type)
        (or (fx/keyword->lifecycle type)
            (when (u/get @*env type) dynamic-env-fn->dynamic))
        (fn? type) lifecycle/dynamic-fn->dynamic))



(fx/mount-renderer
 *state
 (fx/create-renderer
  :middleware (fx/wrap-map-desc assoc :fx/type root)
  :opts {:fx.opt/map-event-handler event-handler
         :fx.opt/type->lifecycle   type->lifecycle}))

#_(when-let [{:keys [view get]} (and (keyword? type) (get-in @*env (u/path :foo)))]
    (lifecycle/create (pp "fn->lc" (fx/fn->lifecycle (fn [desc] (println "in lifecysle") (view (assoc desc :fx/data (get @*state))))))
                      {}))
