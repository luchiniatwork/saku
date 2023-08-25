(ns saku.schemas
  (:require
    [camel-snake-kebab.core :as csk]
    [malli.core :as m]
    [malli.transform :as mt]
    [malli.util :as mu]))

(defn rename-keys
  [m kmap]
  (reduce-kv
    (fn [m' k-src fn-or-k-target]
      (let [v-src (get m k-src)
            [k-target v] (if (fn? fn-or-k-target)
                           (fn-or-k-target v-src)
                           [fn-or-k-target v-src])]
        (-> m'
          (cond-> v (assoc k-target v))
          (dissoc k-src))))
    m kmap))

(defn- -compile-rename-keys-transformer
  [kmap]
  {:leave (if (seq kmap)
            #(rename-keys % kmap)
            identity)})

(defn rename-keys-transformer
  [{:keys [rename-key]}]
  (mt/transformer
    {:decoders
     {:map
      {:compile
       (fn [schema _]
         (-compile-rename-keys-transformer
           (into {}
             (keep (fn [[k-src val-schema]]
                     (when-let [rename (get (m/properties val-schema) rename-key)]
                       [k-src rename])))
             (m/entries schema))))}}}))

(def PolicyType (m/schema [:enum "Resource" "Identity"]))

(def EvaluateOneInput
  (m/schema [:map
             [:drn :string]
             [:actionId :string]
             [:identities [:sequential {:min 1} :string]]]))

(def EvaluateManyInput
  (m/schema [:map
             [:drns [:sequential {:min 1} :string]]
             [:actionId :string]
             [:identities [:sequential {:min 1} :string]]]))

(def StatementInput
  [:map
   [:sid {:optional true
          :output-k :statement/sid} string?]
   [:actionIds {:output-k :statement/actions} [:sequential string?]]
   [:effect {:output-k (fn [x] [:statement/effect [:effect (csk/->kebab-case-keyword x)]])} string?]])

(def ResourceStatementInput
  (mu/merge StatementInput
    [:map
     [:identities {:output-k :statement/identities} [:sequential string?]]]))

(def IdentityStatementInput
  (mu/merge StatementInput
    [:map
     [:resources {:output-k :statement/resources} [:sequential string?]]]))

(def ResourcePolicyInput
  [:map
   [:drn {:output-k :policy/drn} string?]
   [:statements {:output-k :policy/statements} [:sequential {:min 1} ResourceStatementInput]]])

(def IdentityPolicyInput
  [:map
   [:drn {:output-k :policy/drn} string?]
   [:statements {:output-k :policy/statements} [:sequential {:min 1} IdentityStatementInput]]])

(def UpsertPoliciesInput
  [:multi {:dispatch :policyType}
   ["Resource"
    [:map
     [:policyType [:= "Resource"]]
     [:policies [:sequential ResourcePolicyInput]]]]
   ["Identity"
    [:map
     [:policyType [:= "Identity"]]
     [:policies [:sequential IdentityPolicyInput]]]]])

(def StatementOutput
  [:map
   [:statement/sid {:optional true
                    :output-k :sid} string?]
   [:statement/actions {:output-k :actionIds} [:sequential string?]]
   [:statement/effect {:output-k (fn [{:keys [effect]}] [:effect (csk/->SCREAMING_SNAKE_CASE_STRING effect)])} string?]])

(def ResourceStatementOutput
  (mu/merge StatementOutput
    [:map
     [:statement/identities {:output-k :identities} [:sequential string?]]]))

(def IdentityStatementOutput
  (mu/merge StatementOutput
    [:map
     [:statement/resources {:output-k :resources} [:sequential string?]]]))

(def IdentityOrResourcePolicyOutput
  [:map
   [:policy/drn {:output-k :drn} string?]
   [:policy/statements {:output-k :statements}
    [:sequential
     [:multi {:dispatch (fn [statement]
                          (cond
                            (:statement/identities statement) "Resource"
                            (:statement/resources statement) "Identity"))}
      ["Resource" ResourceStatementOutput]
      ["Identity" IdentityStatementOutput]]]]])

(def ResourcePolicyOutput
  (mu/assoc-in IdentityOrResourcePolicyOutput
    [:policy/statements :sequential]
    ResourceStatementOutput))

(def IdentityPolicyOutput
  (mu/assoc-in IdentityOrResourcePolicyOutput
    [:policy/statements :sequential]
    IdentityStatementOutput))

(def UpsertPoliciesOutput
  [:multi {:dispatch :policyType}
   ["Resource"
    [:map
     [:policyType [:= "Resource"]]
     [:policies [:sequential ResourcePolicyOutput]]]]
   ["Identity"
    [:map
     [:policyType [:= "Identity"]]
     [:policies [:sequential IdentityPolicyOutput]]]]])

(def RetractPoliciesInput
  [:map
   [:drns [:sequential string?]]])

(def RetractPoliciesOutput
  [:map
   [:retracted-drns {:output-k :retractedDrns} [:sequential string?]]])

(def AddStatementsInput
  [:multi {:dispatch :policyType}
   ["Resource"
    [:map
     [:policyType [:= "Resource"]]
     [:policy ResourcePolicyInput]]]
   ["Identity"
    [:map
     [:policyType [:= "Identity"]]
     [:policy IdentityPolicyInput]]]])
(def AddStatementsOutput UpsertPoliciesOutput)

(def RetractStatementsInput
  [:map
   [:drn {:output-k :policy/drn} string?]
   [:statementIds {:output-k :statement-ids} [:sequential string?]]])

(def RetractStatementsOutput
  [:map
   [:policy IdentityOrResourcePolicyOutput]])

(def DescribePoliciesInput
  [:map
   [:drns [:sequential string?]]
   [:policyType {:optional true} PolicyType]])

(def DescribePoliciesOutput
  [:map
   [:policies [:sequential IdentityOrResourcePolicyOutput]]])
