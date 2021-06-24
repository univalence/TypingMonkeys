(ns monkey-shell.style.terminal
  (:require [cljfx.css :as css]
            [clojure.pprint :as pprint]
            [cljfx.api :as fx]))

(defn style []
  (css/register ::style
                (let [base-color "#eee"
                      style {:app.style/text-color          base-color
                             :app.style/help-color          (str base-color "8")
                             :app.style/border-color        (str base-color "4")
                             :app.style/shadow-color        (str base-color "3")
                             :app.style/focus-color         (str base-color "8")
                             :app.style/control-color       "#000"
                             :app.style/control-hover-color "#f4f4f4"
                             :app.style/background-color    "#eee"
                             :app.style/spacing             1
                             :app.style/scroll-bar-size     9
                             :app.style/padding             20
                             :app.style/corner-size         5
                             :app.style/label-padding       "2px 4px"}
                      text (fn [size weight]
                             {:-fx-text-fill        "#eee"
                              :-fx-background-color "#000"
                              :-fx-wrap-text        true
                              :-fx-font-weight      weight
                              :-fx-font-size        size

                              })
                      control-shadow (format "dropshadow(gaussian, %s, 5, 0, 0, 1)"
                                             (:app.style/shadow-color style))
                      inner-shadow (format "innershadow(gaussian, %s, 5, 0, 0, 2)"
                                           (:app.style/shadow-color style))
                      hover-shadow (format "dropshadow(gaussian, %s, 7, 0, 0, 2)"
                                           (:app.style/shadow-color style))
                      armed-shadow (format "dropshadow(gaussian, %s, 3, 0, 0, 1)"
                                           (:app.style/shadow-color style))
                      border {:-fx-border-color      (:app.style/border-color style)
                              :-fx-background-color  (:app.style/control-color style)
                              :-fx-border-radius     (:app.style/corner-size style)
                              :-fx-background-radius (:app.style/corner-size style)}
                      button (merge
                               (text 13 :normal)
                               border
                               {:-fx-padding (:app.style/label-padding style)
                                :-fx-effect  control-shadow
                                ":focused"   {:-fx-border-color (:app.style/focus-color style)}
                                ":hover"     {:-fx-effect           hover-shadow
                                              :-fx-background-color (:app.style/control-hover-color style)}
                                ":armed"     {:-fx-effect armed-shadow}})]
                  (merge
                    style
                    {".app"         {"-text-field" (merge
                                                     (text 13 :normal)
                                                     {:-fx-border-color     "transparent"
                                                      :-fx-background-color "transparent"})

                                     "-code"       (merge
                                                     (text 13 :normal)
                                                     {:-fx-font-family "monospace"
                                                      :-fx-padding     (:app.style/spacing style)})

                                     "-pending"    (merge
                                                     (text 13 :normal)
                                                     {:-fx-font-family "monospace"
                                                      :-fx-text-fill   "#a89732"
                                                      :-fx-padding     (:app.style/spacing style)})
                                     "-term-btn"   (merge
                                                     button
                                                     {:-fx-background-color "transparent"
                                                      :-fx-alignment "center"
                                                      ":hover"              {:-fx-effect           hover-shadow
                                                                             :-fx-background-color "#a89732"}}
                                                     )
                                     }
                     ".scroll-pane" (merge
                                      border
                                      {:-fx-effect            inner-shadow
                                       :-fx-focus-traversable true
                                       ":focused"             {:-fx-border-color      (:app.style/focus-color style)
                                                               :-fx-background-insets 0}
                                       "> .viewport"          {:-fx-background-color (:app.style/control-color style)}
                                       "> .corner"            {:-fx-background-color :transparent}})
                     ".scroll-bar"  {:-fx-background-color :transparent
                                     "> .thumb"            {:-fx-background-color  (:app.style/focus-color style)
                                                            :-fx-background-radius (:app.style/scroll-bar-size style)
                                                            :-fx-background-insets 1
                                                            ":pressed"             {:-fx-background-color (:app.style/text-color style)}}
                                     ":horizontal"         {"> .increment-button > .increment-arrow" {:-fx-pref-height (:app.style/scroll-bar-size style)}
                                                            "> .decrement-button > .decrement-arrow" {:-fx-pref-height (:app.style/scroll-bar-size style)}}
                                     ":vertical"           {"> .increment-button > .increment-arrow" {:-fx-pref-width (:app.style/scroll-bar-size style)}
                                                            "> .decrement-button > .decrement-arrow" {:-fx-pref-width (:app.style/scroll-bar-size style)}}
                                     "> .decrement-button" {:-fx-padding         0
                                                            "> .decrement-arrow" {:-fx-shape   nil
                                                                                  :-fx-padding 0}}
                                     "> .increment-button" {:-fx-padding         0
                                                            "> .increment-arrow" {:-fx-shape   nil
                                                                                  :-fx-padding 0}}}}))))