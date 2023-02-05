(ns saku.system.dal
  (:require [saku.dal-datalevin :refer [dal-obj]]
            [integrant.core :as ig]
            [taoensso.timbre :refer [debug info warn error fatal report] :as timbre]))


(defmethod ig/init-key ::dal-obj [_ {:keys [db-conn] :as opts}]
  (info {:msg "Initializing Data Abstraction Layer"})
  (dal-obj opts))
