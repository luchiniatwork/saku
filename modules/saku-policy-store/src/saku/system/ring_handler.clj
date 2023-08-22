(ns saku.system.ring-handler
  (:require
    [integrant.core :as ig]
    [taoensso.timbre :as log]
    [saku.routes :as routes]
    [reitit.ring]))

(defn ring-handler
  [opts]
  (let [routes (routes/api-routes opts)
        router (reitit.ring/router routes)]
    (reitit.ring/ring-handler
      router
      (reitit.ring/create-default-handler))))

(defmethod ig/init-key :handler/ring
  [_ opts]
  (log/info {:msg "Initializing Ring handler"})
  (ring-handler opts))
