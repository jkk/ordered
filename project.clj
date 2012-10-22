(defproject com.jkkramer/ordered "1.3.4"
  :url "https://github.com/jkk/ordered"
  :license {:name "Eclipse Public License - v 1.0"
            :url "http://www.eclipse.org/legal/epl-v10.html"}
  :description "Pure-clojure implementation of ruby's ordered hash and set types - instead of sorting by key, these collections retain insertion order. Fork of flatland/ordered with AOT compilation and no data_readers.clj."
  :dependencies [[org.clojure/clojure "1.4.0"]]
  :aot [ordered.map ordered.set]
  ;; Not required except for benchmarks.
  :profiles {:dev {:dependencies [[ordered-collections "0.4.2"]]}})
