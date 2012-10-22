(ns com.jkkramer.ordered.performance-test
  (:use clojure.test))

(deftest reflection
#_  (binding [*warn-on-reflection* true]
    (are [ns-sym] (= ""
                     (with-out-str
                       (binding [*err* *out*]
                         (require :reload ns-sym))))
         ;; Order of the below is IMPORTANT. set depends on map, and if you
         ;; reload map *after* reloading set, then set refers to classes that
         ;; don't exist anymore, and all kinds of bad stuff happens
         ;; (in this test and others)
         'com.jkkramer.ordered.map 'com.jkkramer.ordered.set)))
