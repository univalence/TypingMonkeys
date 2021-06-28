(ns ape-log.core
  (:require [cljfx.api :as fx]
            [cljfx.css :as css]
            [clojure.string :as str]
            [clojure.walk :as walk]
            [clojure.java.shell :as shell]
            [monkey-shell.style.terminal :as term-style]
            [clojure.string :as str]
            [monkey-shell.components.core :as comps]))

(def *state (atom {
                   :journal-cells   {1 {:content (comps/squared-btn {:h-box/hgrow :always :text "random btn"} :mock)}
                                     2 {}
                                     3 {}}

                   :ui              {:session {:settings {}},
                                     :sidebar {},
                                     :popup   {:content (comps/vbox [(comps/squared-btn {:text "random testing content"} :mock)
                                                                     (comps/squared-btn {:text "terminal"} :mock)
                                                                     (comps/squared-btn {:text "markdown"} :mock)]),
                                               :props   {:showing false, :always-on-top true, :style :undecorated}}},
                   :peer-id         "8e1ca434-4aae-4495-a2dd-215db0c63c3a",
                   :user            {:color       "#FFB366",
                                     :description "blabla",
                                     :pseudo      "papa",
                                     :db/id       "pierrebaille@gmail.com",
                                     :id          "pierrebaille@gmail.com"},
                   :shell-sessions  {:pouet {:members      [{:color       "#FFB366",
                                                             :description "blabla",
                                                             :pseudo      "papa",
                                                             :db/id       "pierrebaille@gmail.com"}],
                                             :last-updater "5e77ced7-598b-4763-b7cc-7f970b5b8739",
                                             :host         {:color       "#FFB366",
                                                            :description "blabla",
                                                            :pseudo      "papa",
                                                            :db/id       "pierrebaille@gmail.com"},
                                             :history      [{:from "pierrebaille@gmail.com",
                                                             :id   "bfebcac7-81aa-4971-a185-734bfe55093e",
                                                             :text "ls -la",
                                                             :pwd  "/Users/pierrebaille/Code/Univalence/firechat",
                                                             :user "pierrebaille\n",
                                                             :out  "total 256
                                           drwxr-xr-x@ 20 pierrebaille  staff    640 Jun 25 10:33 .
                                           drwxr-xr-x  25 pierrebaille  staff    800 Apr 28 10:58 ..
                                           -rw-r--r--@  1 pierrebaille  staff   6148 Mar  9 18:16 .DS_Store
                                           drwxr-xr-x   5 pierrebaille  staff    160 May  5 15:27 .cpcache
                                           drwxr-xr-x  15 pierrebaille  staff    480 Jun 24 10:06 .git
                                           -rw-r--r--   1 pierrebaille  staff    181 Mar 16 14:23 .gitignore
                                           drwxr-xr-x  14 pierrebaille  staff    448 Jun 25 10:33 .idea
                                           -rw-r--r--   1 pierrebaille  staff      5 Jun 25 10:35 .nrepl-port
                                           drwxr-xr-x  10 pierrebaille  staff    320 Jun  1 10:22 .shadow-cljs
                                           -rw-r--r--   1 pierrebaille  staff   2538 Jun  3 10:26 README.md
                                           drwxr-xr-x   5 pierrebaille  staff    160 May  5 17:31 data
                                           -rw-r--r--   1 pierrebaille  staff    689 Jun  9 10:56 deps.edn
                                           -rw-r--r--   1 pierrebaille  staff  23688 Jun 19 18:09 firechat.iml
                                           drwxr-xr-x  97 pierrebaille  staff   3104 May  6 10:24 node_modules
                                           -rw-r--r--   1 pierrebaille  staff    119 Jun  3 10:48 note.org
                                           drwxr-xr-x   4 pierrebaille  staff    128 Jun 24 15:47 out
                                           -rw-r--r--   1 pierrebaille  staff  73300 May  6 11:49 package-lock.json
                                           -rw-r--r--   1 pierrebaille  staff    307 May  6 11:49 package.json
                                           drwxr-xr-x   7 pierrebaille  staff    224 Jun 24 16:55 src
                                           drwxr-xr-x   3 pierrebaille  staff     96 May  5 10:33 target
                                           "}
                                                            {:from "pierrebaille@gmail.com",
                                                             :text "echo yop",
                                                             :id   "a793ec95-b479-480e-82f8-1be8522b92b2",
                                                             :pwd  "/Users/pierrebaille/Code/Univalence/firechat",
                                                             :user "pierrebaille\n",
                                                             :out  "yop\n"}
                                                            {:from "pierrebaille@gmail.com",
                                                             :id   "8a3138da-912e-45d7-a76f-ae14b22c0d60",
                                                             :text "echo uuu",
                                                             :pwd  "/Users/pierrebaille/Code/Univalence/firechat",
                                                             :user "pierrebaille\n",
                                                             :out  "uuu\n"}
                                                            {:from "pierrebaille@gmail.com",
                                                             :text "echo iop",
                                                             :id   "f8d2235f-6276-49cb-927c-15b065254bb3",
                                                             :pwd  "/Users/pierrebaille/Code/Univalence/firechat",
                                                             :user "pierrebaille\n",
                                                             :out  "iop\n"}
                                                            {:from "pierrebaille@gmail.com",
                                                             :text "ls -la",
                                                             :id   "5965402f-544b-47d0-a1da-fa76e13c6b33",
                                                             :pwd  "/Users/pierrebaille/Code/Univalence/firechat",
                                                             :user "pierrebaille\n",
                                                             :out  "total 264
                                           drwxr-xr-x@ 21 pierrebaille  staff    672 Jun 25 16:45 .
                                           drwxr-xr-x  25 pierrebaille  staff    800 Apr 28 10:58 ..
                                           -rw-r--r--@  1 pierrebaille  staff   6148 Mar  9 18:16 .DS_Store
                                           drwxr-xr-x   5 pierrebaille  staff    160 May  5 15:27 .cpcache
                                           drwxr-xr-x  15 pierrebaille  staff    480 Jun 25 16:22 .git
                                           -rw-r--r--   1 pierrebaille  staff    181 Mar 16 14:23 .gitignore
                                           drwxr-xr-x  14 pierrebaille  staff    448 Jun 25 16:44 .idea
                                           -rw-r--r--   1 pierrebaille  staff      5 Jun 25 16:18 .nrepl-port
                                           drwxr-xr-x  10 pierrebaille  staff    320 Jun  1 10:22 .shadow-cljs
                                           -rw-r--r--   1 pierrebaille  staff    398 Jun 25 15:14 README.md
                                           drwxr-xr-x   5 pierrebaille  staff    160 May  5 17:31 data
                                           -rw-r--r--   1 pierrebaille  staff    816 Jun 25 16:21 deps.edn
                                           -rw-r--r--   1 pierrebaille  staff  25756 Jun 25 16:18 firechat.iml
                                           drwxr-xr-x  97 pierrebaille  staff   3104 May  6 10:24 node_modules
                                           -rw-r--r--   1 pierrebaille  staff    119 Jun  3 10:48 note.org
                                           drwxr-xr-x   3 pierrebaille  staff     96 Jun 25 15:16 out
                                           -rw-r--r--   1 pierrebaille  staff  73300 May  6 11:49 package-lock.json
                                           -rw-r--r--   1 pierrebaille  staff    307 May  6 11:49 package.json
                                           drwxr-xr-x   3 pierrebaille  staff     96 Jun 25 16:17 resources
                                           drwxr-xr-x   6 pierrebaille  staff    192 Jun 25 15:16 src
                                           drwxr-xr-x   3 pierrebaille  staff     96 May  5 10:33 target
                                           "}
                                                            {:from "pierrebaille@gmail.com",
                                                             :text "echo bibou",
                                                             :id   "94d9eec8-7852-49d8-a6fe-9ed249768c69",
                                                             :pwd  "/Users/pierrebaille/Code/Univalence/firechat",
                                                             :user "pierrebaille\n",
                                                             :out  "bibou\n"}],
                                             :env          {:OLDPWD           "/Users/pierrebaille/Code/Univalence/firechat",
                                                            :PWD              "/Users/pierrebaille/Code/Univalence/firechat",
                                                            :USER             "pierrebaille",
                                                            :XPC_SERVICE_NAME "0",
                                                            :SHLVL            "1",
                                                            :_                "/usr/bin/env"}}},
                   :focused-session :pouet

                   }))

(defn cell [state]
  (comps/hbox {} [(comps/squared-btn {:pref-width 30 :text "+"} :show-popup)
                  (comps/squared-btn {:pref-width 30 :text "⚙"} :mock)]))

(defn root [state] {:fx/type :stage
                    :showing true
                    :width   1050
                    :height  600
                    :scene   {:fx/type     :scene
                              :stylesheets [(::css/url (term-style/style))]
                              :root        {:fx/type  :v-box
                                            :children [(cell state)]}}})

(defn dynamic-popup [state]
  (comps/window (get-in state [:ui :popup :props])
                (get-in state [:ui :popup :content])))

(defn many [state]
  (comps/many [(root state) (dynamic-popup state)]))

(defn handler
  "HANDLER"
  [event]
  (case (:event/type event)
    :mock (println "TODO")
    :show-popup (swap! *state assoc-in [:ui :popup :props :showing] true)
    ))

(fx/mount-renderer
  *state
  (fx/create-renderer
    :middleware (fx/wrap-map-desc assoc :fx/type many)
    :opts {:fx.opt/map-event-handler handler}))

(println @*state)