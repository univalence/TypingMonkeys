(ns typing-monkeys.components.codemirror.core
  (:require [cljfx.api :as fx]
            [shadow-cljfx.web-view :as wv]))


(def ID :typing-monkeys.components.codemirror)


(defn component
  [{:as opts}]
  (merge
    {:fx/type wv/web-view,
     :id ID,
     :handler (fn [message] (println "received: " message)),
     :on-load (fn [_ _] (println "web view loaded."))}
    (if (:dev opts)
      {:url "http://localhost:8080/typing_monkeys/components/codemirror"}
      {:html (slurp "target/typing_monkeys/components/codemirror/cljfx.html")})
    opts))


(defmacro e>
  [& code]
  (list (list (quote clojure.core/requiring-resolve)
              (list 'quote 'shadow.cljs.devtools.api/cljs-eval))
        ID
        (str (cons (quote do) code))
        {}))


(comment
  (require '[shadow-cljfx.repl :as repl])
  (repl/dev! ID)
  (e> (js/console.log "hey"))
  (e> (typing-monkeys.components.codemirror.view/handle! [:put :value "iop"]))
  (wv/send! ID [:put :value "yo"])
  (repl/render-comp ID)
  (repl/compile-cljfx-html ID))
