(ns com.jkkramer.ordered.common)

(defmacro change! [field f & args]
  `(set! ~field (~f ~field ~@args)))

(defprotocol Compactable
  (compact [this]))
