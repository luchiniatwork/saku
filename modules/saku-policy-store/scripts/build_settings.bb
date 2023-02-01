#!/usr/bin/env bb

(ns script
  (:require [clojure.edn :as edn]
            [camel-snake-kebab.core :refer [->SCREAMING_SNAKE_CASE]]))

(let [build-settings-file (str "build-settings.edn")
      settings (-> build-settings-file
                   slurp
                   edn/read-string)]
  (println "Reading" build-settings-file)
  (doseq [[k v] settings]
    (println (str "::set-output name=" (->SCREAMING_SNAKE_CASE (name k)) "::" v))))
