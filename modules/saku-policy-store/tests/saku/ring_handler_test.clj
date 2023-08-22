(ns saku.ring-handler-test
  (:require
    [clojure.test :refer :all]
    [saku.system.ring-handler :as ring-handler]
    [clojure.data.json :as json]
    [saku.test-utils :as tu]))

(defn req!
  [handler & {:keys [op json]}]
  (let [response (handler (cond-> {:request-method :post
                                   :uri            (format "/api/%s" op)}
                            json
                            (-> (assoc :body (json/write-str json))
                              (assoc-in [:headers "content-type"] "application/json"))))]
    (-> response
      (assoc
        :body-params (some-> response :body slurp (json/read-str :key-fn keyword)))
      (select-keys [:body-params :status]))))

(def resource1-statement
  {:sid        "s1"
   :actionIds  ["a1" "a2"]
   :effect     "ALLOW"
   :identities ["user1"]})

(def resource2-statement
  {:sid        "s2"
   :actionIds  ["a3" "a4"]
   :effect     "DENY"
   :identities ["user2"]})

(def resource1-policy
  {:drn        "resource1"
   :statements [resource1-statement]})

(def resource2-policy
  {:drn        "resource2"
   :statements [resource2-statement]})

(def user1-statement
  {:sid       "s1"
   :actionIds ["a5" "a6"]
   :effect    "ALLOW"
   :resources ["resource1"]})

(def user1-policy
  {:drn        "user1"
   :statements [user1-statement]})

(def user2-statement
  {:sid       "s2"
   :actionIds ["a7" "a8"]
   :effect    "DENY"
   :resources ["resource2"]})

(def user2-policy
  {:drn        "user2"
   :statements [user2-statement]})

(deftest policy-crud-test
  (tu/with-dal-ctx [{:keys [dal-obj]} {}]
    (let [handler (ring-handler/ring-handler {:saku.system.dal/dal-obj dal-obj})]

      (testing "UpsertPolicies"
        (testing "Resource"
          (is (= {:status      200
                  :body-params {:policyType "Resource"
                                :policies   [resource1-policy]}}
                (req! handler
                  :op "UpsertPolicies"
                  :json {:policyType "Resource"
                         :policies   [resource1-policy]}))
            "should return the same policy that we inserted")

          (is (= {:status      200
                  :body-params {:policyType "Resource"
                                :policies   [(assoc resource1-policy
                                               :statements [(assoc resource1-statement
                                                              :actionIds ["a"])])]}}
                (req! handler
                  :op "UpsertPolicies"
                  :json {:policyType "Resource"
                         :policies   [(assoc resource1-policy
                                        :statements [(assoc resource1-statement
                                                       :actionIds ["a"])])]}))
            "should entirely replace the policy on second call"))

        (testing "Identity"
          (is (= {:status      200
                  :body-params {:policyType "Identity"
                                :policies   [user1-policy]}}
                (req! handler
                  :op "UpsertPolicies"
                  :json {:policyType "Identity"
                         :policies   [user1-policy]}))
            "should entirely replace the policy on second call")))

      (testing "AddStatements"
        (is (= {:status      200
                :body-params {:policyType "Identity"
                              :policies   [(update user1-policy :statements conj
                                             user2-statement)]}}
              (req! handler
                :op "AddStatements"
                :json {:policyType "Identity"
                       :policy     (assoc user1-policy
                                     :statements [user2-statement])}))
          "should add a new statement to the identity")

        (is (= {:status      200
                :body-params {:policyType "Identity"
                              :policies   [{:drn        "user1"
                                            :statements [user2-statement
                                                         (assoc user1-statement
                                                           :actionIds ["a5" "a6" "a8"])]}]}}
              (req! handler
                :op "AddStatements"
                :json {:policyType "Identity"
                       :policy     {:drn        "user1"
                                    :statements [(assoc user1-statement
                                                   :actionIds ["a5" "a6" "a8"])]}}))
          "should update an existing statement by sid"))

      (testing "RetractStatements"
        (is (= {:status      200
                :body-params (assoc user1-policy
                               :statements [(assoc user1-statement
                                              :actionIds ["a5" "a6" "a8"])])}
              (req! handler
                :op "RetractStatements"
                :json {:drn          "user1"
                       :statementIds ["s2"]}))
          "should retract the s1 statement"))

      (testing "DescribePolicies"
        (is (= {:status      200
                :body-params {:policies [(assoc user1-policy
                                           :statements
                                           [(assoc user1-statement
                                              :actionIds ["a5" "a6" "a8"])])]}}
              (req! handler
                :op "DescribePolicies"
                :json {:drns ["user1"]}))
          "should return the policies for `user1`")

        (is (= {:status      200
                :body-params {:policies [(assoc resource1-policy
                                           :statements
                                           [(assoc resource1-statement
                                              :actionIds ["a"])])]}}
              (req! handler
                :op "DescribePolicies"
                :json {:drns ["resource1"]}))
          "should return the policies for `resource1`")

        (is (= {:status      200
                :body-params {:policies []}}
              (req! handler
                :op "DescribePolicies"
                :json {:drns ["i-dont-exist"]}))
          "should return empty for non-existent policy"))


      (testing "RetractPolicies"
        (is (= {:status      200
                :body-params {:retractedDrns []}}
              (req! handler
                :op "RetractPolicies"
                :json {:drns ["i-dont-exist"]}))
          "should be a noop for policy drn that does not exist")
        (is (= {:status      200
                :body-params {:retractedDrns ["user1"]}}
              (req! handler
                :op "RetractPolicies"
                :json {:drns ["user1"]}))
          "should retract `user1`'s policy")))))

(deftest policy-evaluation-test
  (tu/with-dal-ctx [{:keys [dal-obj]} {}]
    (let [handler (ring-handler/ring-handler {:saku.system.dal/dal-obj dal-obj})]
      ;; Add test policy data
      (req! handler
        :op "UpsertPolicies"
        :json {:policyType "Identity"
               :policies   [user1-policy]})
      (req! handler
        :op "UpsertPolicies"
        :json {:policyType "Identity"
               :policies   [user2-policy]})
      (req! handler
        :op "UpsertPolicies"
        :json {:policyType "Resource"
               :policies   [resource1-policy]})
      (req! handler
        :op "UpsertPolicies"
        :json {:policyType "Resource"
               :policies   [resource2-policy]})

      (testing "EvaluateOne"
        (is (= {:status      200
                :body-params {:effect "DENY" :nature "IMPLICIT"}}
              (req! handler
                :op "EvaluateOne"
                :json {:drn        "nonexistentresource"
                       :actionId   "anyaction"
                       :identities ["anyuser"]}))
          "should deny implicitly")
        (is (= {:status      200
                :body-params {:effect "DENY" :nature "EXPLICIT"}}
              (req! handler
                :op "EvaluateOne"
                :json {:drn        "resource2"
                       :actionId   "a3"
                       :identities ["user2"]}))
          "should deny explicitly")

        (testing "should allow explicitly"
          (is (= {:status      200
                  :body-params {:effect "ALLOW" :nature "EXPLICIT"}}
                (req! handler
                  :op "EvaluateOne"
                  :json {:drn        "resource1"
                         :actionId   "a1"
                         :identities ["user1"]})))))

      (testing "EvaluateMany"
        (is (= {:status      200
                :body-params [{:drn    "resource1"
                               :result {:effect "DENY"
                                        :nature "IMPLICIT"}}]}
              (req! handler
                :op "EvaluateMany"
                :json {:drns       ["resource1"]
                       :actionId   "a3"
                       :identities ["user2"]}))
          "should deny implicitly")

        (is (= {:status      200
                :body-params [{:drn    "resource2"
                               :result {:effect "DENY"
                                        :nature "EXPLICIT"}}]}
              (req! handler
                :op "EvaluateMany"
                :json {:drns       ["resource2"]
                       :actionId   "a3"
                       :identities ["user2"]}))
          "should deny explicitly")

        (is (= {:status      200
                :body-params [{:drn    "resource1"
                               :result {:effect "ALLOW"
                                        :nature "EXPLICIT"}}]}
              (req! handler
                :op "EvaluateMany"
                :json {:drns       ["resource1"]
                       :actionId   "a1"
                       :identities ["user1"]}))
          "should allow explicitly")))))
