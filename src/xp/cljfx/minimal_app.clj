(ns xp.cljfx.minimal-app
  (:require [cljfx.api :as fx]
            [clojure.core.cache :as cache]
            [typing-monkeys.style.constants])
  (:import (java.text SimpleDateFormat)
           (java.util Date)))

;; minimal app with pure effect handling and context
;; you can push a button to change a label text
;; rerenders on namespace evaluation

(def WORDS ["iop" "top" "flop" "nop"])

;; helpers

(defmacro defc
  "just some syntax for most common components"
  [nam fields & {:as default-opts}]
  (let [sym->kw (comp keyword name)
        fx-type {:fx/type (sym->kw nam)}
        fields (zipmap (map sym->kw fields) fields)]
    `(defn ~nam [~@fields ~'& {:as opts#}]
       (merge ~fx-type
              ~fields
              ~default-opts
              opts#))))

(def time-formatter
  (new SimpleDateFormat "HH:mm:ss"))

(defn now-str []
  (.format time-formatter (new Date)))

;; event-handler

(defmulti handle-event :event/type)

(defmethod handle-event ::change-text
  [{:keys [time fx/context]}]
  {:context (fx/swap-context context assoc :text (rand-nth WORDS) :time time)})

(defmethod handle-event ::interval
  [{:keys [delay event]}]
  {:interval [delay event]})

;; effects

(def effects
  {:interval (fn [[t event] dispatch]
               (future (Thread/sleep t)
                       (dispatch event)
                       (dispatch {:event/type ::interval
                                  :delay t :event event})))})

;; co-effects

(def co-effects
  {:time now-str})

;; views

(defn scene-root [x]
  {:fx/type :stage
   :x       1000
   :y       -500
   :showing true
   :scene   {:fx/type :scene
             :root    x}})

(defc label [text])

(defc button [text])

(defn root-view [{:keys [fx/context]}]
  #_(println "rerendering with context: " (fx/sub-val context identity))
  (scene-root {:fx/type  :v-box
               :padding  50
               :children [(label (fx/sub-val context :time))

                          (label (fx/sub-val context :text) ;; context subscription
                                 :style {:-fx-background-color :lightgray
                                         :-fx-padding          10
                                         :-fx-font             [:oblique :bold 25 :monospace]})

                          (button "change"
                                  :on-action {:event/type ::change-text})

                          (button "tic"
                                  :on-action {:event/type ::interval
                                              :delay      1000
                                              :event      {:event/type ::change-text}})]}))


;; bootstrap

(defonce app
  (fx/create-app (atom (fx/create-context
                        {:text "hello"
                         :time (now-str)}
                        #(cache/lru-cache-factory % :threshold 4096)))
                 :event-handler handle-event
                 :effects effects
                 :co-effects co-effects
                 :desc-fn (fn [_] {:fx/type root-view})))

;; dev only

((:renderer app))

