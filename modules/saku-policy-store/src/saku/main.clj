(ns saku.main
  (:gen-class)
  (:require
    [saku.system.jetty]
    [system-utils.initializer :as utils]))

(defn -main [& args]
  (utils/start-system))
