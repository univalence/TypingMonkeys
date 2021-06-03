(ns typing-monkeys.components.codemirror.view
  (:require [reagent.dom :as rd]
            [reagent.core :as r]
            ["@uiw/react-codemirror" :as rcm]
            ["codemirror/mode/clojure/clojure" :as _clj]
            ["codemirror/keymap/sublime" :as _sub]
            [cljs.reader :as reader]))


(def DEFAULT_STATE
  {:editor   {:value              "(ns user)\n\n(def a (+ 2 1))\n\n(+ a a)\n"
              :options            {:theme "monokai" :mode "clojure" :keymap "sublime"}
              :on-change          (fn [editor changes] (js/console.log "change " editor changes))
              :on-cursor-activity (fn [editor] (js/console.log "cursor " editor (.getCursor editor)))
              }
   :messages []})

(def state (r/atom DEFAULT_STATE))

(def actions
  {:put (fn [state args]
          (reduce (fn [s [k v]]
                    (if (sequential? k) (assoc-in s k v) (assoc s k v)))
                  state
                  (partition 2 args)))})

(defn handle! [[verb & args :as msg]]
  (println "handle " msg)
  #_(js/window.app.send (str "will handle " msg))
  (swap! state update :messages conj (pr-str msg))
  (when-let [f (get actions verb)]
    (swap! state f args)))

(defn root []
  (r/create-class
   {:reagent-render
    (fn []
      (println "render" (:messages @state))
      [:div
       (into [:div] (:messages @state))
       [:> rcm (:editor @state)]])
    :component-did-mount
    (fn [this]
      (println "did mount")
      #_(js/console.log this)
      #_(js/console.log rcm)
      #_(set! js/window.mycm
              (.fromTextArea
               cm
               (js/document.getElementById "editor")
               #js {"lineNumbers"   true,
                    "matchBrackets" true,
                    "mode"          "clojure"})))}))


(defn render
  {:dev/after-load true}
  []
  (rd/render [root] (js/document.getElementById "app")))


(defn init
  {:export true}
  []
  (set! js/window.webView (js-obj "send" (fn [x] (handle! (reader/read-string x)))))
  (render))

(comment
 (handle! [:put [:editor :value] "po"])
 (js/console.log "azer")
 (swap! state assoc-in [:editor :value] "iop = uio"))