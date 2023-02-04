(ns saku.system.seed
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [integrant.core :as ig]
            [taoensso.timbre :refer [debug info warn error fatal report]]))

(defn seed []
  (-> "seed.edn"
      io/resource
      slurp
      edn/read-string))

(defmethod ig/init-key ::seed [_ _]
  (info {:msg "Initializing DB seed"
         :component-id ::seed})
  (seed))
