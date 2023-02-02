(ns user
  (:require [datalevin.core :as d]
            [integrant.core :as ig]
            [integrant.repl :refer [clear go halt prep init reset reset-all]]
            [saku.dal :as dal]
            [system-utils.initializer :as system]))

(integrant.repl/set-prep! system/read-config)

(ig/load-namespaces (system/read-config))

(defn conn []
  (:saku.system.datalevin/conn integrant.repl.state/system))

(comment
  (d/q '[:find ?nation
         :in $ ?alias
         :where
         [?e :aka ?alias]
         [?e :nation ?nation]]
       (d/db (conn))
       "fred")



  (d/q '[:find ?nation
         :in $ ?alias
         :where
         [?e :aka ?alias]
         [?e :nation ?nation]]
       (d/db (conn))
       ["foo"])


  (d/q '[:find (pull ?e [*])
         :in $
         :where
         [?e :name]]
       (d/db (conn)))


  (d/q '[:find [(pull ?p [* {:policy/statements [* {:statement/effect [*]}]}]) ...]
         :in $
         :where
         [?p :policy/drn]]
       (d/db (conn)))


  
  

  (dal/upsert-resource-policies (conn)
                                [{:policy/drn "drn1"
                                  :policy/statements [{:statement/actions ["a1" "a2"]
                                                       :statement/effect [:effect :deny]
                                                       :statement/identities ["user1" "user2"]}]}
                                 {:policy/drn "drn2"
                                  :policy/statements [{:statement/actions ["a3" "a4"]
                                                       :statement/effect [:effect :allow]
                                                       :statement/identities ["user3"]}]}])

  (dal/upsert-identity-policies (conn)
                                [{:policy/drn "user1"
                                  :policy/statements [{:statement/actions ["a1" "a2"]
                                                       :statement/effect [:effect :deny]
                                                       :statement/resources ["drn1" "drn2"]}]}
                                 {:policy/drn "user2"
                                  :policy/statements [{:statement/actions ["a3" "a4"]
                                                       :statement/effect [:effect :allow]
                                                       :statement/resources ["drn5"]}]}])

  (dal/retract-policies (conn) ["1" "2" "drn2" "user1" "user2" "drn1"])


  (dal/get-policies (d/db (conn)) ["drn2" "drn1"])

  (dal/get-resource-policies (d/db (conn)) ["drn2"])

  (dal/get-identity-policies (d/db (conn)) ["user1"])
  
  #_(saku.schemas/assert* saku.schemas/resource-statement [{:statement/actions ["a3" "a4"]
                                                            :statement/effect [:effect :allow]
                                                            :statement/identities ["user3"]}])
  
  )
