(ns saku.system.graphql-resolvers
  (:require [integrant.core :as ig]
            [taoensso.timbre :refer [debug info warn error fatal report] :as timbre]
            ;;[saku.resolvers :as resolvers]
            ))


(defmethod ig/init-key ::resolvers [_ opts]
  (info {:msg "Initializing Lacinia Resolvers"})
  {:MutationRoot/upsertResourcePolicies (constantly {})
   :MutationRoot/upsertIdentityPolicies (constantly {})
   :MutationRoot/retractPolicies (constantly {})

   :QueryRoot/policies (constantly {})
   :QueryRoot/identityPolicies (constantly {})
   :QueryRoot/resourcePolicies (constantly {})
   :QueryRoot/evaluateOne (constantly {})
   :QueryRoot/evaluateMany (constantly {})})


#_(defmethod ig/init-key ::entity-resolvers [_ opts]
    (info {:msg "Initializing Lacinia Entity Resolvers"})
    {;; Internal ones
     :Inspiration (inspirations/get-inspiration-entity opts)
     :IdeationSession (ideation/get-ideation-session-entity opts)
     :Tag (tags/get-tags-entities opts)

     ;; External ones
     :Story (tags/get-story-entities opts)
     :ServerMetadataGroup (metadata/get-server-metadata-group-external opts)
     :User (ideation/get-user-entities opts)})
