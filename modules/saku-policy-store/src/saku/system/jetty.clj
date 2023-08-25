(ns saku.system.jetty
  (:require
    [integrant.core :as ig]
    [ring.adapter.jetty :as jetty]
    [saku.system.ring-handler]
    [kwill.logger :as log]))

(defmethod ig/init-key :saku.system/server
  [_ {:keys [handler jetty-opts]}]
  (log/info {:msg "Initializing Jetty"})
  (jetty/run-jetty handler jetty-opts))

(defmethod ig/halt-key! :saku.system/server
  [_ server]
  (.stop server)
  (log/info {:msg "Stopped HTTP server."}))
