(ns saku.system.graphql-resolvers
  (:require [integrant.core :as ig]
            [taoensso.timbre :refer [debug info warn error fatal report] :as timbre]
            [saku.resolvers :as resolvers]))


(defmethod ig/init-key ::resolvers [_ opts]
  (info {:msg "Initializing Lacinia Resolvers"})
  {:MutationRoot/upsertResourcePolicies (constantly {})
   :MutationRoot/upsertIdentityPolicies (constantly {})
   :MutationRoot/retractPolicies (constantly {})

   :QueryRoot/policies (resolvers/get-policies opts)
   :QueryRoot/identityPolicies (constantly {})
   :QueryRoot/resourcePolicies (constantly {})
   ;;:QueryRoot/evaluateOne (constantly {})
   ;;:QueryRoot/evaluateMany (constantly {})
   })


(defmethod ig/init-key ::entity-resolvers [_ opts]
  (info {:msg "Initializing Lacinia Entity Resolvers"})
  {})
