(ns monkey-shell.style.markdown
  (:require [cljfx.css :as css]
            [clojure.pprint :as pprint]
            [cljfx.api :as fx]))

(defn style []
  (css/register ::style
                {"*"                  {
                                       :-fx-line-spacing 4
                                       :-fx-font-family  "Ubuntu"
                                       :-fx-font-size    14
                                       :-fx-text-fill    "#ddd"
                                       :-fx-fill         "#ddd"}

                 ".root"              {:-fx-background-color "#444"}

                 ".emphasis"          {:-fx-font-style "italic"}

                 ".input"             {:-fx-font-family "Ubuntu Mono"}

                 ".scroll-pane"       {:-fx-background-color      "transparent"
                                       "> .viewport"              {:-fx-background-color "transparent"}

                                       "> .scroll-bar:horizontal" {:-fx-pref-height 10}
                                       "> .scroll-bar:vertical"   {:-fx-pref-width 10}

                                       "> .corner"                {:-fx-background-color "transparent"}
                                       "> .scroll-bar"            {:-fx-background-color "transparent"
                                                                   "> .track"            {:-fx-background-color  "#0002"
                                                                                          :-fx-background-radius 10}

                                                                   "> .thumb"            {:-fx-background-color  "#888"
                                                                                          :-fx-background-radius 10
                                                                                          :-fx-background-insets 0
                                                                                          :-fx-padding           0}}}
                 ".document"          {:-fx-padding 30}

                 ".heading.level-1"   {:-fx-padding "16 0 0 0"
                                       "*"          {:-fx-font-weight "bold"
                                                     :-fx-font-size   30}}

                 ".heading.level-2"   {:-fx-padding "14 0 0 0"
                                       "*"          {:-fx-font-weight "bold"
                                                     :-fx-font-size   26}}

                 ".heading.level-3 "  {:-fx-padding "12 0 0 0"
                                       "*"          {
                                                     :-fx-font-weight "bold"
                                                     :-fx-font-size   22}}

                 ".heading.level-4"   {:-fx-padding "10 0 0 0"
                                       "*"          {
                                                     :-fx-font-weight "bold"
                                                     :-fx-font-size   20}}

                 ".heading.level-5"   {:-fx-padding "8 0 0 0"
                                       "*"          {
                                                     :-fx-font-weight "bold"
                                                     :-fx-font-size   18}}

                 ".paragraph"         {:-fx-padding "10 0 0 0"}

                 ".code"              {:-fx-background-color  "#0002"
                                       :-fx-background-radius 4
                                       :-fx-background-insets -1
                                       "*"                    {:-fx-font-family "Ubuntu Mono"}}

                 ".code-block"        {:-fx-padding           10
                                       :-fx-margin            10
                                       :-fx-background-color  "#0002"
                                       :-fx-background-radius 4
                                       "*"                    {:-fx-font-family "Ubuntu Mono"}}


                 ".hyperlink"         {:-fx-border-color "transparent"
                                       :-fx-padding      0
                                       "*"               {
                                                          :-fx-border-color "transparent"
                                                          :-fx-padding      0
                                                          :-fx-fill         "#9ce"}}

                 ".hyperlink:visited" {:-fx-underline false}
                 ".hyperlink:hover"   {:-fx-underline true}
                 ".md-list"           {:-fx-padding "0 0 0 20"}
                 ".strong-emphasis"   {:-fx-font-weight "bold"}}))
