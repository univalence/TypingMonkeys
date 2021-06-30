(ns monkey-shell.style.mixins)

(def bg-black
  {:-fx-background-color "#000"})

(def text-white
  {:-fx-text-fill "#ddd"})

(defn header [level]
  (let [fs (cond
             (= level 1) 30
             (= level 2) 26
             (= level 3) 22
             (= level 4) 20)
        pt (cond
             (= level 1) 16
             (= level 2) 14
             (= level 3) 12
             (= level 4) 10)]

    {:-fx-font-weight "bold"
     :-fx-font-size   fs
     :-fx-padding     (str pt " 0 0 0")}))

(defn bg [color]
  (let [col-txt (cond
                  (= color "black") "black")]
    {:-fx-background-color col-txt}))

(def code
  {:-fx-background-color  "#0002"
   :-fx-background-radius 4
   :-fx-background-insets -1
   :-fx-font-family       "monospace"})

(def code-block
  {:-fx-padding           "10"
   :-fx-margin            "10"
   :-fx-background-color  "#0002"
   :-fx-background-radius "4"
   :-fx-font-family       "monospace"})

(def scroll-pane
  {:-fx-background-color "transparent"})

(def paragraph {:-fx-padding "10 0 0 0"})

(def strong-emphasis {:-fx-font-style "bold"})
(def emphasis {:-fx-font-style "italic"})
(def md-list {:-fx-padding "0 0 0 20"})
(def hyperlink {:-fx-border-color "transparent"
                :-fx-padding      0
                :-fx-fill         "#9ce"})

#_((def mixin-table
     {:bg-black   bg-black
      :text-white text-white})

   (defn create-style
     "merges all style from vector "
     ([xs]
      (let [maps (mapv (fn [kw] (get mixin-table kw)) xs)]
        (reduce merge maps))))

   (create-style [:bg-black :text-white]))


