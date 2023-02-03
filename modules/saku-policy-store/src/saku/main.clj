(ns saku.main
  (:gen-class)
  (:require [system-utils.initializer :as utils]))

(defn -main [& args]
  (utils/start-system))
