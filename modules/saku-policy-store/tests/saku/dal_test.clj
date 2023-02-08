(ns saku.dal-test
  (:require [clojure.test :refer [deftest testing is are use-fixtures]]
            [saku.dal-interface :as dal]
            [saku.dal-datalevin :as dal-impl]
            [saku.test-utils :as utils]
            [saku.schemas :as schemas]))


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

(defmacro with-dal [spec & body]
  `(utils/with-conn [conn#]
     (let [dal-obj# (dal-impl/dal-obj {:db-conn conn#})
           ~(first spec) dal-obj#]
       ~@body)))

(deftest upserting-policies
  (testing "should upsert resource policies"
    (with-dal [dal-obj]
      (is (= resource-policies (->> resource-policies
                                    (dal/upsert-resource-policies dal-obj)
                                    utils/sanitize-from-db)))))

  (testing "should upsert identity policies"
    (with-dal [dal-obj]
      (is (= identity-policies (->> identity-policies
                                    (dal/upsert-identity-policies dal-obj)
                                    utils/sanitize-from-db)))))

  (testing "should fail on upserting invalid types"
    (with-dal [dal-obj]
      (let [ex (is (thrown? Throwable (dal/upsert-resource-policies dal-obj [{:foo :bar}])))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))
      (let [ex (is (thrown? Throwable (dal/upsert-identity-policies dal-obj [{:foo :bar}])))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))

      (let [ex (is (thrown? Throwable (dal/upsert-resource-policies dal-obj identity-policies)))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))
      (let [ex (is (thrown? Throwable (dal/upsert-identity-policies dal-obj resource-policies)))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))

      (let [ex (is (thrown? Throwable (dal/upsert-resource-policies dal-obj (merge resource-policies
                                                                                   identity-policies))))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category))))
      (let [ex (is (thrown? Throwable (dal/upsert-identity-policies dal-obj (merge resource-policies
                                                                                   identity-policies))))]
        (is (= ::schemas/invalid-type (-> ex ex-data :anomaly/category)))))))


(deftest retract-policies
  (testing "should retract properly"
    (with-dal [dal-obj]
      (dal/upsert-resource-policies dal-obj resource-policies)
      (let [retracted-drns (dal/retract-policies dal-obj ["drn1" "drn2" "non-existing"])]
        (is (= #{"drn1" "drn2"} retracted-drns))
        (is (= #{} (dal/retract-policies dal-obj ["drn1" "drn2"])))))))


(deftest get-policies
  (testing "should return empty on empty case"
    (with-dal [dal-obj]
      (is (= [] (dal/get-policies dal-obj ["foo" "bar"])))
      (dal/upsert-resource-policies dal-obj resource-policies)
      (dal/upsert-identity-policies dal-obj identity-policies)
      (is (= [] (dal/get-policies dal-obj ["foo" "bar"])))))

  (testing "should return existing ones"
    (with-dal [dal-obj]
      (dal/upsert-resource-policies dal-obj resource-policies)
      (dal/upsert-identity-policies dal-obj identity-policies)
      (is (= (set
              (conj []
                    (some #(when (= "drn2" (:policy/drn %)) %) resource-policies)
                    (some #(when (= "user1" (:policy/drn %)) %) identity-policies)))
             (set
              (utils/sanitize-from-db (dal/get-policies dal-obj
                                                        ["drn2" "user1"]))))))))


(deftest get-resource-policies
  (testing "should return empty on empty case"
    (with-dal [dal-obj]
      (is (= [] (dal/get-resource-policies dal-obj ["foo" "bar"])))
      (dal/upsert-resource-policies dal-obj resource-policies)
      (is (= [] (dal/get-resource-policies dal-obj ["foo" "bar"])))))

  (testing "should return existing ones"
    (with-dal [dal-obj]
      (dal/upsert-resource-policies dal-obj resource-policies)
      (is (= [(some #(when (= "drn2" (:policy/drn %)) %) resource-policies)]
             (utils/sanitize-from-db (dal/get-policies dal-obj
                                                       ["drn2" "user1"])))))))


(deftest get-identity-policies
  (testing "should return empty on empty case"
    (with-dal [dal-obj]
      (is (= [] (dal/get-identity-policies dal-obj ["foo" "bar"])))
      (dal/upsert-identity-policies dal-obj identity-policies)
      (is (= [] (dal/get-identity-policies dal-obj ["foo" "bar"])))))

  (testing "should return existing ones"
    (with-dal [dal-obj]
      (dal/upsert-identity-policies dal-obj identity-policies)
      (is (= [(some #(when (= "user1" (:policy/drn %)) %) identity-policies)]
             (utils/sanitize-from-db (dal/get-policies dal-obj
                                                       ["drn2" "user1"])))))))
