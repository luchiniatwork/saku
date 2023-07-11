(ns saku.utils
  (:require [clojure.string :as s]))

(defn ^:private regex-star [text]
  (re-pattern (str "(?i)^" (-> text
                             (s/replace #"(\\|\.|\+|\?|\^|\$|\(|\)|\[|\]|\{|\}|\|)" (fn [[_ c]] (str "\\" c)))
                             (s/replace #"\*" ".*")))))

(defn star-match [^String a ^String b]
  (when (and (string? a) (string? b))
    (boolean (or (re-matches (regex-star a) b)
               (re-matches (regex-star b) a)))))

(defn star-match-one-to-many [a coll]
  (some #(star-match a %) coll))

(defn star-match-many-to-many [coll-a coll-b]
  (some #(some (partial star-match %) coll-a)
    coll-b))

(defn star-match-target [^String star ^String target]
  (boolean (re-matches (regex-star star) target)))

(defn star-match-target-one-to-many [a coll]
  (some #(star-match-target a %) coll))

(defn star-match-target-many-to-many [coll-a coll-b]
  (some #(some (partial star-match-target %) coll-a)
    coll-b))
