(ns typing-monkeys.components.monaco.view
  (:require [reagent.dom :as rd]
            [reagent.core :as r]
            ["@monaco-editor/loader" :default loader]))

;; premier essai avec monaco,
;; fonctionne dans le browser mais pas fans les webViews... (même le site de démo de l'éditeur n'affiche rien)
;; Abandon

(def state (r/atom {:message "Hello shadow-cljx"}))

(defn monaco-init! []
  (.then (.init loader)
         (fn [monaco]
           (set! js/window.editor
                 (.create (.-editor ^js monaco)
                          (js/document.getElementById "editor")
                          #js {"value" "// comment" "language" "javascript"}))

           (js/console.log js/window.editor)
           (.onDidChangeCursorPosition
            js/window.editor
            (fn [x] (println "iop") (js/console.log "change cursor pos: \n" x)))
           (.onDidChangeModelContent
            js/window.editor
            (fn [x] (js/console.log "change model content:\n" x))))))

(defn root
  []
  (r/create-class
   {:render
    (fn [_] [:div [:h1 (:message (clojure.core/deref state))]
             [:div#editor {:style {:height 500 :with 500 :border "none"}}]
             [:button {:on-click (fn [_] (js/window.app.send "toc toc"))}
              "knockin on server's door !"]])

    :component-did-mount
    (fn [_] (monaco-init!))}))


(defn render
  {:dev/after-load true}
  []
  (rd/render [root] (js/document.getElementById "app")))



(defn init
  {:export true}
  []
  (set! js/window.webView
        (js-obj "send" (fn [data] (swap! state assoc :message data))))
  (render))
