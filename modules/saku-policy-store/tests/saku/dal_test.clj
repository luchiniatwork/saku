(ns saku.dal-test
  (:require [clojure.test :refer [deftest testing is are use-fixtures]]
            [saku.dal :as dal]
            [saku.test-utils :as utils]
            [saku.schemas :as schemas]
            [saku.system.datalevin :as db-sys]
            [saku.system.seed :as seed-sys]))


(def resource-policies [{:policy/drn "drn1"
                         :policy/statements [{:statement/actions ["a1" "a2"]
                                              :statement/effect [:effect :deny]
                                              :statement/identities ["user1" "user2"]}]}
                        {:policy/drn "drn2"
                         :policy/statements [{:statement/actions ["a3" "a4"]
                                              :statement/effect [:effect :allow]
                                              :statement/identities ["user3"]}]}])

(def identity-policies [{:policy/drn "user1"
                         :policy/statements [{:statement/actions ["a1" "a2"]
                                              :statement/effect [:effect :deny]
                                              :statement/resources ["drn1" "drn2"]}]}
                        {:policy/drn "user2"
                         :policy/statements [{:statement/actions ["a3" "a4"]
                                              :statement/effect [:effect :allow]
                                              :statement/resources ["drn5"]}]}])


(deftest upserting-policies
  (testing "should upsert resource policies"
    (utils/with-conn [conn]
      (is (= resource-policies (->> resource-policies
                                    (dal/upsert-resource-policies conn)
                                    utils/sanitize-from-db)))))

  (testing "should upsert identity policies"
    (utils/with-conn [conn]
      (is (= identity-policies (->> identity-policies
                                    (dal/upsert-identity-policies conn)
                                    utils/sanitize-from-db)))))

  (testing "should fail on upserting invalid types"
    (utils/with-conn [conn]
      (let [ex (is (thrown? Throwable (dal/upsert-resource-policies conn [{:foo :bar}])))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))
      (let [ex (is (thrown? Throwable (dal/upsert-identity-policies conn [{:foo :bar}])))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))

      (let [ex (is (thrown? Throwable (dal/upsert-resource-policies conn identity-policies)))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))
      (let [ex (is (thrown? Throwable (dal/upsert-identity-policies conn resource-policies)))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))

      (let [ex (is (thrown? Throwable (dal/upsert-resource-policies conn (merge resource-policies
                                                                                identity-policies))))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))
      (let [ex (is (thrown? Throwable (dal/upsert-identity-policies conn (merge resource-policies
                                                                                identity-policies))))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category)))))))


(deftest retract-policies
  (testing "should retract properly"
    (utils/with-conn [conn]
      (dal/upsert-resource-policies conn resource-policies)
      (let [retracted-drns (dal/retract-policies conn ["drn1" "drn2" "non-existing"])]
        (is (= #{"drn1" "drn2"} retracted-drns))
        (is (= #{} (dal/retract-policies conn ["drn1" "drn2"])))))))


(deftest get-policies
  (testing "should return empty on empty case"
    (utils/with-conn [conn]
      (is (= [] (dal/get-policies (dal/db conn) ["foo" "bar"])))
      (dal/upsert-resource-policies conn resource-policies)
      (dal/upsert-identity-policies conn identity-policies)
      (is (= [] (dal/get-policies (dal/db conn) ["foo" "bar"])))))

  (testing "should return existing ones"
    (utils/with-conn [conn]
      (dal/upsert-resource-policies conn resource-policies)
      (dal/upsert-identity-policies conn identity-policies)
      (is (= (set
              (conj []
                    (some #(when (= "drn2" (:policy/drn %)) %) resource-policies)
                    (some #(when (= "user1" (:policy/drn %)) %) identity-policies)))
             (set
              (utils/sanitize-from-db (dal/get-policies (dal/db conn)
                                                        ["drn2" "user1"]))))))))


(deftest get-resource-policies
  (testing "should return empty on empty case"
    (utils/with-conn [conn]
      (is (= [] (dal/get-resource-policies (dal/db conn) ["foo" "bar"])))
      (dal/upsert-resource-policies conn resource-policies)
      (is (= [] (dal/get-resource-policies (dal/db conn) ["foo" "bar"])))))

  (testing "should return existing ones"
    (utils/with-conn [conn]
      (dal/upsert-resource-policies conn resource-policies)
      (is (= [(some #(when (= "drn2" (:policy/drn %)) %) resource-policies)]
             (utils/sanitize-from-db (dal/get-policies (dal/db conn)
                                                       ["drn2" "user1"])))))))


(deftest get-identity-policies
  (testing "should return empty on empty case"
    (utils/with-conn [conn]
      (is (= [] (dal/get-identity-policies (dal/db conn) ["foo" "bar"])))
      (dal/upsert-identity-policies conn identity-policies)
      (is (= [] (dal/get-identity-policies (dal/db conn) ["foo" "bar"])))))

  (testing "should return existing ones"
    (utils/with-conn [conn]
      (dal/upsert-identity-policies conn identity-policies)
      (is (= [(some #(when (= "user1" (:policy/drn %)) %) identity-policies)]
             (utils/sanitize-from-db (dal/get-policies (dal/db conn)
                                                       ["drn2" "user1"])))))))
