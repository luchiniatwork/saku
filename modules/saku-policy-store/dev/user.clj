(ns user
  (:require [datalevin.core :as d]
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [system-utils.initializer :as system]))

(integrant.repl/set-prep! system/read-config)

(ig/load-namespaces (system/read-config))

#_(defn conn []
    (:system-datahike.db/conn integrant.repl.state/system))
