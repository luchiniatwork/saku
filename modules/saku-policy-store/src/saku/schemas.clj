(ns saku.schemas
  (:require [malli.core :as m]))

(def effect
  (m/schema [:tuple [:= :effect] [:enum :deny :allow]]))

(def identity-statement
  (m/schema [:map
             [:statement/actions [:sequential :string]]
             [:statement/effect effect]
             [:statement/resources [:sequential :string]]]))

(def resource-statement
  (m/schema [:map
             [:statement/actions [:sequential :string]]
             [:statement/effect effect]
             [:statement/identities [:sequential :string]]]))

(def resource-policy
  (m/schema [:map
             [:policy/drn :string]
             [:policy/statements [:sequential {:min 1} resource-statement]]]))

(def resource-policies
  (m/schema [:sequential resource-policy]))

(def identity-policy
  (m/schema [:map
             [:policy/drn :string]
             [:policy/statements [:sequential {:min 1} identity-statement]]]))

(def identity-policies
  (m/schema [:sequential identity-policy]))

(def evaluate-one-args
  (m/schema [:map
             [:drn :string]
             [:actionId :string]
             [:identities [:sequential {:min 1} :string]]]))

(def evaluate-many-args
  (m/schema [:map
             [:drns [:sequential {:min 1} :string]]
             [:actionId :string]
             [:identities [:sequential {:min 1} :string]]]))

(defn assert*
  ([schema obj]
   (when-not (m/validate schema obj)
     (throw (ex-info "Invalid type" {:anomaly/category ::invalid-type
                                     :schema schema
                                     :obj obj
                                     :cause (m/explain schema obj)})))))
