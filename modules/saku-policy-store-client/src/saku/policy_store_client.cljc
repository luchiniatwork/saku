(ns saku.policy-store-client
  (:require [re-graph.core :as re-graph]
            [promesa.core :as p]
            #?(:cljs ["xhr2" :as xhr2])
            #?(:cljs [goog.object :as g])))

#?(:cljs
   (set! js/XMLHttpRequest xhr2))


(def ^:private server-meta-query
  [:serverMeta "query ServerMeta {
  serverMeta { version environmentId } }"])

(def ^:private policies-query
  [:policies "query Policies($drns: [ID!]!) {
  policies(drns: $drns) {
    drn
    ... on IdentityPolicyDocument {
      drn
      statements {
        sid
        actionIds
        effect
        resources
      }
    }
    ... on ResourcePolicyDocument {
      drn
      statements {
        sid
        actionIds
        effect
        identities
      }
    }
  }
}"])

(def ^:private resource-policies-query [:resourcePolicies "query ResourcePolicies($drns: [ID!]!) {
  resourcePolicies(drns: $drns) {
    drn
    statements {
      sid
      actionIds
      effect
      identities
    }
  }
}"])


(def ^:private identity-policies-query
  [:identityPolicies "query IdentityPolicies($drns: [ID!]!) {
  identityPolicies(drns: $drns) {
    drn
    statements {
      sid
      actionIds
      effect
      resources
    }
  }
}"])

(def ^:private upsert-resource-policies-mutation
  [:upsertResourcePolicies "mutation UpsertResourcePolicies($policies: [ResourcePolicyInputDocument!]!) {
  upsertResourcePolicies(policies: $policies) {
    drn
    statements {
      sid
      actionIds
      effect
      identities
    }
  }
}"])

(def ^:private upsert-identity-policies-mutation
  [:upsertIdentityPolicies "mutation UpsertIdentityPolicies($policies: [IdentityPolicyInputDocument!]!) {
  upsertIdentityPolicies(policies: $policies) {
    drn
    statements {
      sid
      actionIds
      effect
      resources
    }
  }
}"])

(def ^:private add-identity-statements-mutation
  [:addIdentityStatements "mutation AddIdentityStatements($policy: IdentityPolicyInputDocument!) {
  addIdentityStatements(inputPolicy: $policy) {
    drn
    statements {
      sid
      actionIds
      effect
      resources
    }
  }
}"])

(def ^:private add-resource-statements-mutation
  [:addResourceStatements "mutation AddResourceStatements($policy: ResourcePolicyInputDocument!) {
  addResourceStatements(inputPolicy: $policy) {
    drn
    statements {
      sid
      actionIds
      effect
      identities
    }
  }
}"])

(def ^:private retract-statements-mutation
  [:retractStatements "mutation RetractStatements($drn: ID!, $statementIds: [String!]!) {
  retractStatements(drn: $drn, statementIds: $statementIds) {
    drn
  }
}"])

(def ^:private retract-policies-mutation
  [:retractPolicies "mutation RetractPolicies($drns: [ID!]!) {
  retractPolicies(drns: $drns)
}"])

(def ^:private evaluate-one-query
  [:evaluateOne "query EvaluateOne($actionId: String!, $drn: ID!, $identities: [ID!]!) {
  evaluateOne(actionId: $actionId, drn: $drn, identities: $identities) {
    effect
    nature
  }
}"])

(def ^:private evaluate-many-query
  [:evaluateMany "query EvaluateMany($actionId: String!, $drns: [ID!]!, $identities: [ID!]!) {
  evaluateMany(actionId: $actionId, drns: $drns, identities: $identities) {
    drn
    result {
      effect
      nature
    }
  }
}"])

(defn connect [url]
  (re-graph/init {:ws nil
                  :http {:url url}}))


(defn ^:private callback-fn [kind promise field]
  (let [err-msg (case kind
                  :query "Query errors"
                  :mutation "Mutation errors")]
    (fn [{:keys [data errors] :as params}]
      (if errors
        (p/reject! promise (ex-info (str err-msg ". " errors) {:errors errors}))
        (p/resolve! promise (get data field))))))

(defn server-meta []
  (let [promise (p/deferred)
        [field query] server-meta-query]
    (re-graph/query {:query query
                     :callback (callback-fn :query promise field)})
    promise))

(defn policies [drns]
  (let [promise (p/deferred)
        [field query] policies-query]
    (re-graph/query {:query query
                     :variables {:drns drns}
                     :callback (callback-fn :query promise field)})
    promise))

(defn resource-policies [drns]
  (let [promise (p/deferred)
        [field query] resource-policies-query]
    (re-graph/query {:query query
                     :variables {:drns drns}
                     :callback (callback-fn :query promise field)})
    promise))

(defn identity-policies [drns]
  (let [promise (p/deferred)
        [field query] identity-policies-query]
    (re-graph/query {:query query
                     :variables {:drns drns}
                     :callback (callback-fn :query promise field)})
    promise))

(defn upsert-resource-policies [policies]
  (let [promise (p/deferred)
        [field mutation] upsert-resource-policies-mutation]
    (re-graph/mutate {:query mutation
                      :variables {:policies policies}
                      :callback (callback-fn :mutation promise field)})
    promise))

(defn upsert-identity-policies [policies]
  (let [promise (p/deferred)
        [field mutation] upsert-identity-policies-mutation]
    (re-graph/mutate {:query mutation
                      :variables {:policies policies}
                      :callback (callback-fn :mutation promise field)})
    promise))

(defn add-identity-statements [policy]
  (let [promise (p/deferred)
        [field mutation] add-identity-statements-mutation]
    (re-graph/mutate {:query mutation
                      :variables {:policy policy}
                      :callback (callback-fn :mutation promise field)})
    promise))

(defn add-resource-statements [policy]
  (let [promise (p/deferred)
        [field mutation] add-resource-statements-mutation]
    (re-graph/mutate {:query mutation
                      :variables {:policy policy}
                      :callback (callback-fn :mutation promise field)})
    promise))

(defn retract-statements [retract-statements-input]
  (let [promise (p/deferred)
        [field mutation] retract-statements-mutation]
    (re-graph/mutate {:query mutation
                      :variables retract-statements-input
                      :callback (callback-fn :mutation promise field)})
    promise))

(defn retract-policies [drns]
  (let [promise (p/deferred)
        [field mutation] retract-policies-mutation]
    (re-graph/mutate {:query mutation
                      :variables {:drns drns}
                      :callback (callback-fn :mutation promise field)})
    promise))

(defn evaluate-one [{:keys [drn actionId identities] :as args}]
  (let [promise (p/deferred)
        [field query] evaluate-one-query]
    (re-graph/query {:query query
                     :variables args
                     :callback (callback-fn :query promise field)})
    promise))

(defn evaluate-many [{:keys [drns actionId identities] :as args}]
  (let [promise (p/deferred)
        [field query] evaluate-many-query]
    (re-graph/query {:query query
                     :variables args
                     :callback (callback-fn :query promise field)})
    promise))

(defn disconnect []
  (re-graph/destroy {}))

#?(:cljs
   (defn server-meta-js []
     (p/let [out (server-meta)]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn policies-js [drns]
     (p/let [out (policies drns)]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn resource-policies-js [drns]
     (p/let [out (resource-policies drns)]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn identity-policies-js [drns]
     (p/let [out (identity-policies drns)]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn upsert-resource-policies-js [policies]
     (p/let [out (upsert-resource-policies (js->clj policies :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn upsert-identity-policies-js [policies]
     (p/let [out (upsert-identity-policies (js->clj policies :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn add-identity-statements-js [policy]
     (p/let [out (add-identity-statements (js->clj policy :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn add-resource-statements-js [policy]
     (p/let [out (add-resource-statements (js->clj policy :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn retract-statements-js [retract-statements-input]
     (p/let [out (retract-statements (js->clj retract-statements-input :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn retract-policies-js [drns]
     (p/let [out (retract-policies drns)]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn evaluate-one-js [params]
     (p/let [out (evaluate-one (js->clj params :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn evaluate-many-js [params]
     (p/let [out (evaluate-many (js->clj params :keywordize true))]
       (p/promise (clj->js out)))))

#?(:cljs
   (defn is-resource-policy-js [data]
     (boolean
      (some-> data
              (g/get "statements")
              first
              (g/get "identities")))))

#?(:cljs
   (defn is-identity-policy-js [data]
     (boolean
      (some-> data
              (g/get "statements")
              first
              (g/get "resources")))))

#?(:cljs
   (defn is-policy-document-js [data]
     (boolean
      (or (is-resource-policy-js data)
          (is-identity-policy-js data)))))

(comment

 (connect "http://localhost:8080/api")

 (deref (server-meta))

 (deref (policies ["drn1"]))

 (deref (resource-policies ["drn1"]))

 (deref (identity-policies ["userdrn"]))

 (deref (upsert-resource-policies [{:drn "drn1"
                                    :statements
                                    [{:sid "a1"
                                      :actionIds ["a" "b"]
                                      :effect :ALLOW
                                      :identities ["i1"]}]}]))

  @(retract-statements {:drn           "drn1"
                        :statementIds ["a1" "a2"]})
  @(add-resource-statements {:drn          "drn1"
                             :statements [{:sid "a2"
                                           :actionIds ["a2" "b2"]
                                           :effect :ALLOW
                                           :identities ["i2"]}]})


 @(retract-policies ["drn2" "drn1"])

 @(upsert-identity-policies [{:drn "userdrn"
                              :statements [{:actionIds ["*"]
                                            :resources ["*"]
                                            :effect :ALLOW}]}])

  @(add-identity-statements {:drn          "userdrn"
                             :statements [{:sid "u1"
                                           :actionIds ["a2" "b2"]
                                           :effect :ALLOW
                                           :resources ["r1"]}]})
  @(retract-statements {:drn           "userdrn"
                        :statementIds ["r1"]})

 @(evaluate-one {:drn "resdrn"
                 :actionId "action"
                 :identities ["userdrn"]})

 @(evaluate-many {:drns ["resdrn"]
                  :actionId "action"
                  :identities ["userdrn"]})

 @(evaluate-many {:drns ["resdrn", "resdrn2"]
                  :actionId "action"
                  :identities ["userdrn"]})

 (disconnect))
