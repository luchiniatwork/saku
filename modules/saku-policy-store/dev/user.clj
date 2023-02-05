(ns user
  (:require [datalevin.core :as d]
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            #_[saku.dal :as dal]
            [system-utils.initializer :as system]))

(integrant.repl/set-prep! system/read-config)

(ig/load-namespaces (system/read-config))

(defn conn []
  (:saku.system.datalevin/conn integrant.repl.state/system))

(comment


  
  


  (dal/retract-policies (conn) ["1" "2" "drn2" "user1" "user2" "drn1"])


  (dal/get-policies (d/db (conn)) ["drn2" "drn1"])

  (dal/get-resource-policies (d/db (conn)) ["drn2"])

  (dal/get-identity-policies (d/db (conn)) ["user1"])
  
  #_(saku.schemas/assert* saku.schemas/resource-statement [{:statement/actions ["a3" "a4"]
                                                            :statement/effect [:effect :allow]
                                                            :statement/identities ["user3"]}])
  
  )
