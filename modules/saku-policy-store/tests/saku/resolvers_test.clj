(ns saku.resolvers-test
  (:require [clojure.test :refer [deftest testing is are use-fixtures]]
            [saku.resolvers :as resolvers]
            [saku.schemas :as schemas]
            [saku.dal-interface :as dal]))

(def dal-obj {:impl :mock})

(def regex-resource #"^resource.*$")

(def regex-identity #"^user.*$")

(def resource-db-doc
  {:db/id 23
   :policy/drn 'drn
   :policy/statements [{:statement/effect {:effect :ALLOW
                                           :db/id 20}
                        :statement/actions ["a1" "a2"]
                        :statement/identities ["user1"]}
                       {:statement/effect {:effect :DENY
                                           :db/id 21}
                        :statement/actions ["a3" "a4"]
                        :statement/identities ["user2"]}]})

(def resource-doc
  {:drn 'drn
   :statements [{:effect :ALLOW
                 :actionIds ["a1" "a2"]
                 :identities ["user1"]}
                {:effect :DENY
                 :actionIds ["a3" "a4"]
                 :identities ["user2"]}]})

(def identity-db-doc
  {:db/id 27
   :policy/drn 'drn
   :policy/statements [{:statement/effect {:effect :ALLOW
                                           :db/id 20}
                        :statement/actions ["a5" "a6"]
                        :statement/resources ["resource5"]}
                       {:statement/effect {:effect :DENY
                                           :db/id 21}
                        :statement/actions ["a7" "a8"]
                        :statement/resources ["resource2"]}]})

(def identity-doc
  {:drn 'drn
   :statements [{:effect :ALLOW
                 :actionIds ["a5" "a6"]
                 :resources ["resource5"]}
                {:effect :DENY
                 :actionIds ["a7" "a8"]
                 :resources ["resource2"]}]})

(def db
  {regex-resource
   [resource-db-doc
    resource-doc]

   regex-identity
   [identity-db-doc
    identity-doc]})


(defn find-db-drns [drns]
  (reduce (fn [c drn]
            (let [entry (some (fn [[k [payload _]]]
                                (when (re-matches k drn)
                                  payload))
                              db)]
              (cond-> c
                entry (conj (assoc entry :policy/drn drn)))))
          [] drns))

(defn find-doc-drns [drns]
  (reduce (fn [c drn]
            (let [entry (some (fn [[k [_ payload]]]
                                (when (re-matches k drn)
                                  payload))
                              db)]
              (cond-> c
                entry (conj (assoc entry :drn drn)))))
          [] drns))


(defmethod dal/db :mock [_]
  nil)

(defmethod dal/get-policies :mock [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(dal/db obj) db-or-drns])]
    (find-db-drns drns')))

(defmethod dal/get-resource-policies :mock [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(dal/db obj) db-or-drns])]
    (find-db-drns (filter #(re-matches regex-resource %) drns'))))

(defmethod dal/get-identity-policies :mock [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(dal/db obj) db-or-drns])]
    (find-db-drns (filter #(re-matches regex-identity %) drns'))))

(defmethod dal/upsert-resource-policies :mock [_ policies]
  (->> policies
       (map (fn [p] (:policy/drn p)))
       find-db-drns))

(defmethod dal/upsert-identity-policies :mock [_ policies]
  (->> policies
       (map (fn [p] (:policy/drn p)))
       find-db-drns))

(defmethod dal/retract-policies :mock [_ drns]
  (->> drns
       (filter #(or (= "drn23" %)
                    (= "drn69" %)))
       set))


(deftest queries
  (testing "get-policies query"
    (let [r (resolvers/get-policies {:dal-obj dal-obj})]
      (is (= (find-doc-drns ["resourcedrn" "userdrn"])
             (r nil {:drns ["resourcedrn" "userdrn"]} nil)))))

  (testing "get-resource-policies query"
    (let [r (resolvers/get-resource-policies {:dal-obj dal-obj})]
      (is (= (find-doc-drns ["resourcedrn1" "resource2"])
             (r nil {:drns ["resourcedrn1" "resource2" "userdrn"]} nil)))))

  (testing "get-identity-policies query"
    (let [r (resolvers/get-identity-policies {:dal-obj dal-obj})]
      (is (= (find-doc-drns ["user1" "user2"])
             (r nil {:drns ["resourcedrn1" "resource2" "user1" "user2"]} nil)))))

  (testing "getting version"
    (let [r (resolvers/server-meta {:version {:environment-id :test
                                              :version "1234"}})]
      (is (= {:environmentId "test"
              :version "1234"}
             (r nil nil nil))))))


(deftest mutations
  (testing "upsert-resource-policies"
    (let [r (resolvers/upsert-resource-policies {:dal-obj dal-obj})]
      (is (= (find-doc-drns ["resource1" "resource2"])
             (r nil {:policies (find-doc-drns ["resource1" "resource2"])} nil)))))

  (testing "upsert-identity-policies"
    (let [r (resolvers/upsert-identity-policies {:dal-obj dal-obj})]
      (is (= (find-doc-drns ["user3" "user6"])
             (r nil {:policies (find-doc-drns ["user3" "user6"])} nil)))))

  (testing "retract-policies"
    (let [r (resolvers/retract-policies {:dal-obj dal-obj})]
      (is (= #{} (r nil {:drns ["drn1" "drn2"]} nil)))
      (is (= #{"drn23"} (r nil {:drns ["drn1" "drn23"]} nil)))
      (is (= #{"drn23" "drn69"} (r nil {:drns ["drn1" "drn23" "drn69"]} nil))))))


(deftest evaluate-one
  (testing "should deny implicitely"
    (let [r (resolvers/evaluate-one {:dal-obj dal-obj})
          {:keys [effect nature]} (r nil {:drn "nonexistentresource"
                                          :actionId "anyaction"
                                          :identities ["anyuser"]}
                                     nil)]
      (is (= :DENY effect))
      (is (= :IMPLICIT nature))))

  (testing "should deny explicitly"
    (let [r (resolvers/evaluate-one {:dal-obj dal-obj})
          {:keys [effect nature]} (r nil {:drn "resource1"
                                          :actionId "a3"
                                          :identities ["user2"]}
                                     nil)]
      (is (= :DENY effect))
      (is (= :EXPLICIT nature)))
    (let [r (resolvers/evaluate-one {:dal-obj dal-obj})
          {:keys [effect nature]} (r nil {:drn "resource2"
                                          :actionId "a7"
                                          :identities ["user2"]}
                                     nil)]
      (is (= :DENY effect))
      (is (= :EXPLICIT nature))))

  (testing "should allow explicitly"
    (let [r (resolvers/evaluate-one {:dal-obj dal-obj})
          {:keys [effect nature]} (r nil {:drn "resource1"
                                          :actionId "a1"
                                          :identities ["user1"]}
                                     nil)]
      (is (= :ALLOW effect))
      (is (= :EXPLICIT nature)))
    (let [r (resolvers/evaluate-one {:dal-obj dal-obj})
          {:keys [effect nature]} (r nil {:drn "resource5"
                                          :actionId "a5"
                                          :identities ["user1"]}
                                     nil)]
      (is (= :ALLOW effect))
      (is (= :EXPLICIT nature))))

  (testing "should reject wrong calls"
    (are [invalid-args]
        (let [r (resolvers/evaluate-one {:dal-obj dal-obj})
              ex (is (thrown? Throwable (r nil invalid-args nil)))]
          (= ::schemas/invalid-type (-> ex ex-data :anomaly/category)))
      nil {} {:drn ""} {:actionId ""})))


(deftest evaluate-many
  (testing "should deny implicitely"
    (let [r (resolvers/evaluate-many {:dal-obj dal-obj})
          drns ["nonexistentresource1"
                "nonexistentresource2"]
          result-set (r nil {:drns drns
                             :actionId "anyaction"
                             :identities ["anyuser"]}
                        nil)]
      (doseq [{:keys [drn result]} result-set]
        (is (get (set drns) drn))
        (is (= :DENY (:effect result)))
        (is (= :IMPLICIT (:nature result))))))

  (testing "should deny explicitly"
    (let [r (resolvers/evaluate-many {:dal-obj dal-obj})
          drns ["resource1"]
          result-set (r nil {:drns drns
                             :actionId "a3"
                             :identities ["user2"]}
                        nil)]
      (println result-set)
      (doseq [{:keys [drn result]} result-set]
        (is (get (set drns) drn))
        (is (= :DENY (:effect result)))
        (is (= :EXPLICIT (:nature result)))))
    (let [r (resolvers/evaluate-many {:dal-obj dal-obj})
          drns ["resource2"]
          result-set(r nil {:drns drns
                            :actionId "a7"
                            :identities ["user2"]}
                       nil)]
      (doseq [{:keys [drn result]} result-set]
        (is (get (set drns) drn))
        (is (= :DENY (:effect result)))
        (is (= :EXPLICIT (:nature result))))))

  (testing "should allow explicitly"
    (let [r (resolvers/evaluate-many {:dal-obj dal-obj})
          drns ["resource1"]
          result-set (r nil {:drns drns
                             :actionId "a1"
                             :identities ["user1"]}
                        nil)]
      (doseq [{:keys [drn result]} result-set]
        (is (get (set drns) drn))
        (is (= :ALLOW (:effect result)))
        (is (= :EXPLICIT (:nature result)))))
    (let [r (resolvers/evaluate-many {:dal-obj dal-obj})
          drns ["resource5"]
          result-set (r nil {:drns drns
                             :actionId "a5"
                             :identities ["user1"]}
                        nil)]
      (doseq [{:keys [drn result]} result-set]
        (is (get (set drns) drn))
        (is (= :ALLOW (:effect result)))
        (is (= :EXPLICIT (:nature result))))))

  (testing "should reject wrong calls"
    (are [invalid-args]
        (let [r (resolvers/evaluate-many {:dal-obj dal-obj})
              ex (is (thrown? Throwable (r nil invalid-args nil)))]
          (= ::schemas/invalid-type (-> ex ex-data :anomaly/category)))
      nil {} {:drns ""} {:actionId ""} {:drns "x" :actionId "a" :identities ["i"]})))
