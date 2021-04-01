(ns monk.core
  (:refer-clojure :exclude [< > get])
  (:require [monk.flow :as flow]
            [monk.lenses-c :as lenses]
            [monk.utils :as u :refer [import-defn+]]))

(def at flow/at)
(def path lenses/path)
(def ? lenses/?)

(u/import-defn+ flow/>)
(u/import-defn+ flow/<)
(u/import-defn+ lenses/get)
(u/import-defn+ lenses/upd)
(u/import-defn+ lenses/upd<)
(u/import-defn+ lenses/put)
(u/import-defn+ lenses/pass)

(comment

 (> {:a 1}
    {(? :b) 1}))



