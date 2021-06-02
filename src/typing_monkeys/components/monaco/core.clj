(ns typing-monkeys.components.monaco.core
  (:require [cljfx.api :as fx]
            [shadow-cljfx.web-view :as wv]))


(def ID :typing-monkeys.components.monaco)


(defn component
  [{:as opts}]
  (merge {:fx/type wv/web-view,
          :id      ID,
          :handler (fn [message] (println "received: " message)),
          :on-load (fn [_ _] (println "web view loaded."))}
         (if (:dev opts)
           {:url "http://localhost:8080/typing_monkeys/text/monaco"}
           {:html (slurp "target/typing_monkeys/text/monaco/cljfx.html")})
         opts))


(defmacro e>
  [& code]
  (list (list (quote clojure.core/requiring-resolve)
              (quote shadow.cljs.devtools.api/cljs-eval))
        ID
        (str (cons (quote do) code))
        {}))


(comment

 (require '[shadow-cljfx.repl :as repl])

 (repl/dev! ID)
 (e> (js/console.log "hey"))
 (wv/send! ID "hello")

 (repl/compile-cljfx-html ID)
 (repl/render-comp ID))
