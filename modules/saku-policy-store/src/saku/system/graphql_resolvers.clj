(ns saku.system.graphql-resolvers
  (:require [integrant.core :as ig]
            [taoensso.timbre :refer [debug info warn error fatal report] :as timbre]
            [saku.resolvers :as resolvers]))


(defmethod ig/init-key ::resolvers [_ opts]
  (info {:msg "Initializing Lacinia Resolvers"})
  {:MutationRoot/upsertResourcePolicies (resolvers/upsert-resource-policies opts)
   :MutationRoot/upsertIdentityPolicies (resolvers/upsert-identity-policies opts)
   :MutationRoot/retractPolicies (resolvers/retract-policies opts)

   :QueryRoot/policies (resolvers/get-policies opts)
   :QueryRoot/identityPolicies (resolvers/get-identity-policies opts)
   :QueryRoot/resourcePolicies (resolvers/get-resource-policies opts)

   :QueryRoot/evaluateOne (resolvers/evaluate-one opts)
   :QueryRoot/evaluateMany (resolvers/evaluate-many opts)

   :QueryRoot/serverMeta (resolvers/server-meta opts)})


(defmethod ig/init-key ::entity-resolvers [_ opts]
  (info {:msg "Initializing Lacinia Entity Resolvers"})
  {})
