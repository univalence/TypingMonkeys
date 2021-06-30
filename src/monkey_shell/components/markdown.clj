(ns monkey-shell.components.markdown
  (:require [cljfx.api :as fx]
            [clojure.string :as str]
            [clojure.core.cache :as cache]
            [cljfx.css :as css]
            [monkey-shell.style.markdown :as markdown-style]
            [monkey-shell.components.core :as comps]
            [monkey-shell.style.terminal :as term-style]
            [monkey-shell.style.mixins :as mx])
  (:import [de.codecentric.centerdevice.javafxsvg SvgImageLoaderFactory]
           [de.codecentric.centerdevice.javafxsvg.dimension PrimitiveDimensionProvider]
           [java.awt Desktop]
           [java.io File]
           [java.net URI]
           [org.commonmark.node Node]
           [org.commonmark.parser Parser]))

(SvgImageLoaderFactory/install (PrimitiveDimensionProvider.))

(def state
  (atom
    {:typed-text (slurp "./README.md")
     :editing    false}))

(defn commonmark->clj [^Node node]
  (let [tag (->> node
                 .getClass
                 .getSimpleName
                 (re-seq #"[A-Z][a-z]+")
                 (map str/lower-case)
                 (str/join "-")
                 keyword)
        all-attrs (->> node
                       bean
                       (map (fn [[k v]]
                              [(->> k
                                    name
                                    (re-seq #"[A-Z]?[a-z]+")
                                    (map str/lower-case)
                                    (str/join "-")
                                    keyword)
                               v]))
                       (into {}))]
    {:tag      tag
     :attrs    (dissoc all-attrs :next :previous :class :first-child :last-child :parent)
     :children (->> node
                    .getFirstChild
                    (iterate #(.getNext ^Node %))
                    (take-while some?)
                    (mapv commonmark->clj))}))

(defn node-sub [text]
  (println "text " text)
  (-> (Parser/builder)
      .build
      (.parse text)
      commonmark->clj))

(defmulti handle-event :event/type)

(defmethod handle-event :default [e]
  (prn e))

(defmethod handle-event ::type-text [{:keys [fx/event]}]
  (swap! state assoc :typed-text event))

(defmethod handle-event ::click-edit [{:keys [fx/event]}]
  (swap! state assoc :editing (if (:editing @state) false true)))

(defmulti md->fx :tag)

(defn md-view [{:keys [node]}]
  (md->fx node))

(defmethod md->fx :heading [{children :children {:keys [level]} :attrs}]
  {:fx/type  :text-flow
   #_(:style-class ["heading" (str "level-" level)])
   :style    (mx/header level)
   :children (for [node children]
               (md-view {:node node}))})

(defmethod md->fx :paragraph [{children :children}]
  {:fx/type  :text-flow
   #_(:style-class "paragraph")
   :style    mx/paragraph
   :children (for [node children]
               {:fx/type md-view
                :node    node})})

(defmethod md->fx :text [{{:keys [literal]} :attrs}]
  {:fx/type    :text
   :cache      true
   :cache-hint :speed
   :text       literal})

(defmethod md->fx :code [{{:keys [literal]} :attrs}]
  {:fx/type    :label
   :cache      true
   :cache-hint :speed
   :style      mx/code
   :text       literal})

(defmethod md->fx :fenced-code-block [{{:keys [literal]} :attrs}]
  {:fx/type  :v-box
   :padding  {:top 9}
   :children [{:fx/type      :scroll-pane
               :style        (merge mx/scroll-pane mx/code-block)
               #_(:style-class ["scroll-pane" "code-block"])
               :fit-to-width true
               :content      {:fx/type    :label
                              :cache      true
                              :cache-hint :speed
                              :max-width  ##Inf
                              :min-width  :use-pref-size
                              :text       literal}}]})

(defmethod md->fx :indented-code-block [{{:keys [literal]} :attrs}]
  {:fx/type  :v-box
   :padding  {:top 9}
   :children [{:fx/type      :scroll-pane
               :style        (merge mx/scroll-pane mx/code-block)
               :fit-to-width true
               :content      {:fx/type    :label
                              :cache      true
                              :cache-hint :speed
                              :max-width  ##Inf
                              :min-width  :use-pref-size
                              :text       literal}}]})

(defmethod md->fx :link [{{:keys [^String destination]} :attrs children :children}]
  (let [link {:fx/type   :hyperlink
              :on-action (fn [_]
                           (future
                             (try
                               (if (str/starts-with? destination "http")
                                 (.browse (Desktop/getDesktop) (URI. destination))
                                 (.open (Desktop/getDesktop) (File. destination)))
                               (catch Exception e
                                 (.printStackTrace e)))))}]
    (if (and (= 1 (count children))
             (= :text (:tag (first children))))
      (assoc link :text (-> children first :attrs :literal))
      (assoc link :graphic {:fx/type  :h-box
                            :children (for [node children]
                                        {:fx/type md-view
                                         :node    node})}))))

(defmethod md->fx :strong-emphasis [{:keys [children]}]
  (if (and (= 1 (count children))
           (= :text (:tag (first children))))
    {:fx/type    :text
     :cache      true
     :cache-hint :speed
     #_(:style-class "strong-emphasis")
     :style      mx/strong-emphasis

     :text       (-> children first :attrs :literal)}
    {:fx/type     :h-box
     :cache       true
     #_(:style-class "strong-emphasis")
     :style      mx/strong-emphasis
     :children    (for [node children]
                    {:fx/type md-view
                     :node    node})}))

(defmethod md->fx :emphasis [{:keys [children]}]
  (if (and (= 1 (count children))
           (= :text (:tag (first children))))
    {:fx/type     :text
     :cache       true
     :cache-hint  :speed
     #_(:style-class "emphasis")
     :style      mx/emphasis
     :text        (-> children first :attrs :literal)}
    {:fx/type     :h-box
     #_(:style-class "emphasis")
     :style      mx/emphasis
     :children    (for [node children]
                    {:fx/type md-view
                     :node    node})}))

(defmethod md->fx :soft-line-break [_]
  {:fx/type :text
   :text    " "})

(defmethod md->fx :document [{:keys [children]}]
  {:fx/type     :v-box
   :style-class "document"
   :children    (for [node children]
                  (md-view {:node node}))})

(defmethod md->fx :image [{{:keys [destination]} :attrs}]
  {:fx/type :image-view
   :image   {:url                (if (str/starts-with? destination "http")
                                   destination
                                   (str "file:" destination))
             :background-loading true}})

(defmethod md->fx :bullet-list [{{:keys [bullet-marker]} :attrs children :children}]
  {:fx/type     :v-box
   #_(:style-class "md-list")
   :style      mx/md-list
   :children    (for [node children]
                  {:fx/type   :h-box
                   :alignment :baseline-left
                   :spacing   4
                   :children  [{:fx/type    :label
                                :min-width  :use-pref-size
                                :cache      true
                                :cache-hint :speed
                                :text       (str bullet-marker)}
                               {:fx/type md-view
                                :node    node}]})})

(defmethod md->fx :ordered-list [{{:keys [delimiter start-number]} :attrs
                                  children                         :children}]
  {:fx/type     :v-box
   #_(:style-class "md-list")
   :style      mx/md-list
   :children    (map (fn [child number]
                       {:fx/type   :h-box
                        :alignment :baseline-left
                        :spacing   4
                        :children  [{:fx/type    :label
                                     :cache      true
                                     :cache-hint :speed
                                     :min-width  :use-pref-size
                                     :text       (str number delimiter)}
                                    (assoc (md->fx child)
                                      :h-box/hgrow :always)]})
                     children
                     (range start-number ##Inf))})

(defmethod md->fx :list-item [{:keys [children]}]
  {:fx/type  :v-box
   :children (for [node children]
               {:fx/type md-view
                :node    node})})

(defmethod md->fx :default [{:keys [tag attrs children]}]
  {:fx/type  :v-box
   :children [{:fx/type    :label
               :cache      true
               :cache-hint :speed
               :style      {:-fx-background-color :red}
               :text       (str tag " " attrs)}
              {:fx/type  :v-box
               :padding  {:left 10}
               :children (for [node children]
                           (md-view {:node node}))}]})

(defn note-input [state]
  {:fx/type         :text-area
   :style-class     "input"
   :v-box/vgrow     :always
   :text            (:typed-text state)
   :on-text-changed {:event/type ::type-text :fx/sync true}})

(defn note-preview [state]
  {:fx/type      :scroll-pane
   :fit-to-width true
   :content      (md-view {:node (node-sub (:typed-text state))})})

(defn root [state]
  {:fx/type :stage
   :showing true
   :scene   {:fx/type :scene
             #_(:stylesheets ["monkey_shell/style/markdown.css"])
             :root    (if (:editing state) (comps/vbox [(comps/hbox [(comps/squared-btn {:pref-width 100
                                                                                         :text       "EDIT"} ::click-edit)])
                                                        (note-preview state)])
                                           (comps/vbox [(comps/hbox [(comps/squared-btn {:pref-width 100
                                                                                         :text       "PREVIEW"} ::click-edit)])
                                                        (note-input state)]))}})

(def renderer
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type root)
    :opts {:fx.opt/map-event-handler handle-event}))

(fx/mount-renderer state renderer)