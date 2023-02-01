#!/usr/bin/env bb

(ns script
  (:require [clojure.java.io :as io]
            [clojure.java.shell :as shell]
            [clojure.string :as string])
  (:import (java.time LocalDate)))

(def ^:private calver-patterns
  {#"YYYY" #(format "%04d" (.getYear %))
   #"YY"   #(-> (.getYear %)
                str (subs 2 4)
                Integer/parseInt
                str)
   #"0Y"   #(format "%02d" (-> % .getYear
                               str (subs 2 4)
                               Integer/parseInt))
   #"MM"   #(->> % .getMonthValue str)
   #"0M"   #(->> % .getMonthValue (format "%02d"))
   #"DD"   #(->> % .getDayOfMonth str)
   #"0D"   #(->> % .getDayOfMonth (format "%02d"))})

(def ^:private default-calver "YYYY.0M.0D")

(defn calver
  "Returns a calver (https://calver.org) based on `LocalDate` and
  following the format specified in `format-str`:

  - YYYY - Full year - 2006, 2016, 2106
  - YY - Short year - 6, 16, 106
  - 0Y - Zero-padded year - 06, 16, 106
  - MM - Short month - 1, 2 ... 11, 12
  - 0M - Zero-padded month - 01, 02 ... 11, 12
  - WW - Short week (since start of year) - 1, 2, 33, 52
  - 0W - Zero-padded week - 01, 02, 33, 52
  - DD - Short day - 1, 2 ... 30, 31
  - 0D - Zero-padded day - 01, 02 ... 30, 31

  Uses the ISO-8601 definition, where a week starts on Monday and the
  first week has a minimum of 4 days.

  The default format string is `YYYY.0M.0D`.

  Calling this function without args uses now LocalDate and the
  default format.

  Calling this function with one arg the arg can be either a LocalDate
  object or a string format."
  ([]
   (calver default-calver))
  ([one-arg]
   (if (= LocalDate (type one-arg))
     (calver one-arg default-calver)
     (calver (LocalDate/now) one-arg)))
  ([date format-str]
   (reduce (fn [a [pattern xfn]]
             (string/replace a pattern (xfn date)))
           format-str calver-patterns)))

(defn sha
  "Returns the current commit sha. By default it will return just the 7
  first characters. You can specify more if you need to."
  ([]
   (sha 7))
  ([length]
   (let [{:keys [exit err out]} (shell/sh "git" "rev-parse" (str "--short=" length) "HEAD")]
     (when (not= 0 exit)
       (throw (ex-info "sha failed" {:reason err})))
     (string/trim out))))

(defn assert-clean?
  "Throws if the repo is not clean."
  [version]
  (let [{:keys [exit err out]} (shell/sh "git" "status" "-s")]
    (when (not= 0 exit)
      (throw (ex-info "assert-clean? failed" {:anomaly/category :cmd-failure
                                              :reason err})))
    (when (not (empty? out))
      (throw (ex-info "git repo is not clean" {:anomaly/category :repo-not-clean
                                               :reason out})))
    version))

(defn unclean-status
  ([]
   (unclean-status "unclean"))
  ([micro-str]
   (try
     (assert-clean? nil)
     ""
     (catch Exception _
       micro-str))))

(defn append
  "Appends the value to the version in the pipe."
  [version value]
  (str version value))

(defn gen []
  (-> (calver "YY.0M.0D")
      (append (str "-" (sha)))
      (append (unclean-status "-UNCLEAN"))))

(let [version (gen)]
  (println "Version:" version)
  (println "Saving to resources/version.txt")
  (spit (io/file "resources/version.txt")
        version)
  (println (str "::set-output name=VERSION::" version)))
