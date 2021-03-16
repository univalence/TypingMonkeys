(ns cljsfx.core
  (:require [reagent.dom :as dom]
            [reagent.core :as rea]))

(defn ^:export init []
  (dom/render [:div "hello reagent"]
              (js/document.getElementById "app")))