(ns cljsfx.xp
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as fx.ext.web-view])
  (:import [javafx.scene.web WebEvent]))

(def *state
  (atom
   {:title nil
    :status nil}))

(defn view2 [{:keys [title status]}]
  {:fx/type :stage
   :showing true
   :title (str title)
   :scene
            {:fx/type :scene
             :root
                      {:fx/type :v-box
                       :children
                                [{:fx/type fx.ext.web-view/with-engine-props
                                  :desc {:fx/type :web-view}
                                  :props {:content (slurp "resources/cljsfx.html")
                                          :on-error #(println "something wrong " %)
                                          :java-script-enabled true
                                          :on-title-changed #(swap! *state assoc :title %)
                                          :on-status-changed #(swap! *state assoc :status (.getData ^WebEvent %))}}
                                 {:fx/type :label
                                  :text (str status)}]}}})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc #'view2)))

(fx/mount-renderer *state renderer)