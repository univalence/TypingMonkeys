(ns typing-monkeys.text.codemirror
  (:require [cljfx.api :as fx]
            [cljfx.ext.web-view :as fx.ext.web-view]
            [cljfx.prop :as prop]
            [cljfx.mutator :as mutator]
            [cljfx.lifecycle :as lifecycle]
            [cljfx.coerce :as coerce])
  (:import [javafx.scene.web WebEvent WebView WebEngine]
           (javafx.concurrent Worker$State)
           [netscape.javascript JSObject]
           (org.w3c.dom.events EventListener)
           (javafx.beans.value ChangeListener ObservableValue)))

(println Worker$State/SUCCEEDED)

(do :html-elements
    (defn script [href]
      (str "<script src=\"" href "\"></script>"))

    (defn style [href]
      (str "<link rel=\"stylesheet\" href=\"" href "\">"))

    (defn simple-html [head body]
      (str
       "<!doctype html>"
       "<html>"
       "<head>" head "</head>"
       "<body>" body "</body>"
       "</html>"))

    (def CODEMIRROR_INIT
      "<script>
        var editor = CodeMirror.fromTextArea(document.getElementById(\"code\"), {
          lineNumbers: true,
          matchBrackets: true,
          mode: \"clojure\"
          });
       </script>")

    (def JS_CODE "function myScript(){return 100;}\\n")
    (def CLJ_CODE "(defn script [href]\n  (str \"<script src=\\\"\" href \"\\\"></script>\"))")

    (def TEXTAREA
      (str "<textarea id=\"code\" name=\"code\">"
           CLJ_CODE
           "</textarea>"))

    (def CODEMIRROR_OUTDATED_ASSETS
      (str (style "http://codemirror.net/lib/codemirror.css")
           (script "http://codemirror.net/lib/codemirror.js")))

    (def CODEMIRROR_ASSETS
      (str (style "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.60.0/codemirror.min.css")
           (style "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.60.0/theme/3024-day.min.css")
           (script "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.60.0/codemirror.min.js")
           (script "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.60.0/mode/clojure/clojure.min.js")
           (script "https://cdnjs.cloudflare.com/ajax/libs/codemirror/5.60.0/keymap/sublime.min.js")))

    (def ALERT_BUTTON
      (str "<button id=\"alert-button\" onclick=\"alert(\"clicked\")\">alert</button>"))

    (def BUTTON
      "<button onclick=\"getElementById('demo').innerHTML = Date()\">What is the time?</button>")

    (def ALERT "<script> alert(\"foo\")</script>"))

(def html2 (simple-html
            CODEMIRROR_ASSETS
            (str ALERT_BUTTON
                 TEXTAREA
                 CODEMIRROR_INIT)))

#_(spit "data/static-mirror.html" html2)

(defn- coerce-content [x]
  (cond
    (string? x) {:content x :content-type "text/html"}
    (and (:content x) (:content-type x)) x))

(defn listener [f]
  (reify EventListener
    (handleEvent [this event]
      (f this event))))

(def ext-with-web-engine
  (fx/make-ext-with-props

   {:content  (prop/make
               (mutator/setter
                #(.loadContent (.getEngine ^WebView %1) (:content %2) (:content-type %2)))
               lifecycle/scalar
               :coerce coerce-content)

    :on-alert (prop/make
               (mutator/setter
                #(.setOnAlert (.getEngine ^WebView %1) %2))
               lifecycle/event-handler
               :coerce coerce/event-handler)

    :on-load  (prop/make
               (mutator/setter
                (fn [this f]
                  (let [engine (.getEngine ^WebView this)]
                    (.addListener (.stateProperty (.getLoadWorker engine))
                                  (proxy [ChangeListener] []
                                    (changed [^ObservableValue ov
                                              ^Worker$State old-state
                                              ^Worker$State new-state]
                                      (if (= new-state Worker$State/SUCCEEDED)
                                        (f engine (.getDocument engine)))))))))
               lifecycle/scalar)}))

(def web-view
  {:fx/type ext-with-web-engine
   :props   {:content  html2
             ;; we can receive message from JS via calling alert
             :on-alert (fn [& xs] (println "alert " xs))
             ;; we can register some listeners when page is loaded
             :on-load  (fn [engine document]
                         (comment (.executeScript engine "editor.on(\"change\",function(cm,change){alert(JSON.stringify(cm.getValue()))})")
                                  (.executeScript engine "editor.on(\"change\",function(cm,change){alert(JSON.stringify(change))})"))
                         (let [alert-button (.getElementById document "alert-button")]
                           (.addEventListener alert-button "click"
                                              (listener (fn [_ ev] (println ev) (.executeScript engine "alert(\"yop\")")))
                                              false)))
             }
   :desc    {:fx/type :web-view}})

(defn view2 [{:keys [title status]}]
  {:fx/type :stage
   :x       1000 :y -1000
   :showing true
   :title   (str title)
   :scene
            {:fx/type :scene
             :root
                      {:fx/type  :v-box
                       :children [web-view
                                  {:fx/type :label
                                   :text    (str status)}]}}})

(def renderer
  (fx/create-renderer
   :middleware (fx/wrap-map-desc #'view2)))

(def *state
  (atom
   {:title  nil
    :status nil}))

(fx/mount-renderer *state renderer)