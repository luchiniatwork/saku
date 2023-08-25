(ns saku.system.version
  (:require [clojure.java.io :as io]
            [clojure.string :as s]
            [integrant.core :as ig]
            [kwill.logger :as log]
            [orzo.core :as orzo]
            [orzo.git :as git]))


(defmethod ig/init-key ::dynamic-gen [_ {:keys [environment-id] :as opts}]
  (log/info {:msg            "Initializing Version - dynamically generating"
             :environment-id environment-id})
  (let [version (-> (git/sha)
                  (orzo/prepend "sha-")
                  (orzo/append (git/unclean-status "-dirty")))]
    (log/info {:msg     "Calculated version"
               :version version})
    {:environment-id environment-id
     :version        version}))


(defmethod ig/init-key ::read-from-disk [_ {:keys [environment-id] :as opts}]
  (log/info {:msg            "Initializing Version - reading from file"
             :environment-id environment-id})
  (let [version (-> "version.txt" io/resource slurp s/split-lines first)]
    (log/info {:msg     "Read version"
               :version version})
    {:environment-id environment-id
     :version        version}))
