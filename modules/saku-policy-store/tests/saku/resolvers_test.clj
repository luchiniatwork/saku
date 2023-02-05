(ns saku.resolvers-test
  (:require [clojure.test :refer [deftest testing is are use-fixtures]]
            [saku.resolvers :as resolvers]
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



(defmethod dal/get-policies :mock [_ drns & [db]]
  (find-db-drns drns))

(defmethod dal/get-resource-policies :mock [_ drns & [db]]
  (find-db-drns (filter #(re-matches regex-resource %) drns)))

(defmethod dal/get-identity-policies :mock [_ drns & [db]]
  (find-db-drns (filter #(re-matches regex-identity %) drns)))

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
             (r nil {:drns ["resourcedrn1" "resource2" "user1" "user2"]} nil))))))


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
