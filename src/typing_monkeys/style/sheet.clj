(ns typing-monkeys.style.sheet
  (:require [typing-monkeys.utils.cljfx :refer [stylesheet]]))

(def main
  (stylesheet ::main
              #_#_#_#_".button"
              {:text-fill        :gray
               :font-family      :monospace
               :font-weight      :bold
               :padding          2
               :margin           20
               :border-color     :gray
               :background-color :transparent
               :border-width     2
               :border-radius    4
               ":hover"          {:text-fill    :tomato
                                  :border-color :tomato}}

              ;; test
              ".notification" {:background-color :black
                               ":hover"          {:background-color :gray}

                               :>                {:text-fill        :white
                                                  :background-color :purple
                                                  ":hover"          {:background-color :yellow}
                                                  ".danger"         {:text-fill :red}
                                                  ".info"           {:text-fill :gray}}}))