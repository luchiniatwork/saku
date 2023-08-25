(ns saku.system.dal
  (:require [integrant.core :as ig]
            [kwill.logger :as log]
            [saku.dal-datalevin :refer [dal-obj]]))


(defmethod ig/init-key ::dal-obj [_ {:keys [db-conn] :as opts}]
  (log/info {:msg "Initializing Data Abstraction Layer"})
  (dal-obj opts))
