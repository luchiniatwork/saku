(ns saku.utils-test
  (:require
    [clojure.test :refer [deftest is]]
    [saku.utils :as utils]))

(deftest star-match-test
  (is (= true (utils/star-match "a" "a"))
    "exact match")
  (is (= false (utils/star-match "a" "b"))
    "exact non-match")
  (is (= true (utils/star-match "a*" "ab"))
    "wildcard match")
  (is (= false (utils/star-match "a*" "bac"))
    "wildcard miss")
  (is (= true (utils/star-match "a:*b|c|d:*" "a:b|c|d:1"))
    "wildcard with regex special character"))
