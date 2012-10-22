## Fork

This is a fork of [flatland's ordered](https://github.com/flatland/ordered). Changes made:

* AOT compilation, so that ordered collections can be used in macros
* Removed `data_readers.clj` to avoid conflicts
* Changed namespaces to `com.jkkramer.ordered` to avoid conflicts

## Overview

ordered provides sets and maps that maintain the insertion order of their contents.

## Sets

    (use 'com.jkkramer.ordered.set)

    (ordered-set 4 3 1 8 2)
    => #ordered/set (4 3 1 8 2)

    (conj (ordered-set 9 10) 1 2 3)
    => #ordered/set (9 10 1 2 3)

    (into (ordered-set) [7 6 1 5 6])
    => #ordered/set (7 6 1 5)

    (disj (ordered-set 8 1 7 2 6) 7)
    => #ordered/set (8 1 2 6)

## Maps

    (use 'com.jkkramer.ordered.map)

    (ordered-map :b 2 :a 1 :d 4)
    => #ordered/map ([:b 2] [:a 1] [:d 4])

    (assoc (ordered-map :b 2 :a 1 :d 4) :c 3)
    => #ordered/map ([:b 2] [:a 1] [:d 4] [:c 3])

    (into (ordered-map) [[:c 3] [:a 1] [:d 4]])
    => #ordered/map ([:c 3] [:a 1] [:d 4])

    (dissoc (ordered-map :c 3 :a 1 :d 4) :a)
    => #ordered/map ([:c 3] [:d 4])
