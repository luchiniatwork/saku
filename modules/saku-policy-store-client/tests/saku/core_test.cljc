(ns saku.core-test
  (:require [saku.core :as core]
            [clojure.test :refer [deftest testing is are use-fixtures]]))

(def reqs
  {;; explicit deny: that one stream doesn't accept reads from alice
   :explicit-deny-resource-based {:action "streams/ReadStream"
                                  :drn "domain.streams.stream/my-stream"
                                  :resource-policy {:statements
                                                    [{:actions ["security/*"]
                                                      :identities ["domain.security.role/ops"]
                                                      :effect :ALLOW}
                                                     {:actions ["streams/readStream" "streams/listStreams"]
                                                      :identities ["domain.security.role/alice" "domain.security.role/bob"]
                                                      :effect :DENY}]}
                                  :identity-policies-map {"domain.security.role/alice" nil}}

   ;; explicit deny: alice can't create subscriptions on any subscritpion
   :explicit-deny-identity-based {:action "streams/CreateSubscription"
                                  :drn "domain.streams.subscription/my-sub"
                                  :resource-policy nil
                                  :identity-policies-map {"domain.security.role/alice" {:statements
                                                                                        [{:actions ["security/*"]
                                                                                          :resources ["domain.security.role/*"]
                                                                                          :effect :ALLOW}
                                                                                         {:actions ["streams/*Subscription*"]
                                                                                          :resources ["domain.streams.subscription/*"]
                                                                                          :effect :DENY}]}}}

   ;; explicit allow: resource-based - stream can be read by anyone
   :explicit-allow-resource-based {:action "streams/ReadStream"
                                   :drn "domain.streams.stream/my-stream"
                                   :resource-policy {:statements
                                                     [{:actions ["streams/ReadStream" "streams/ListStreams"]
                                                       :identities ["*"]
                                                       :effect :ALLOW}]}
                                   :identity-policies-map {"domain.security.role/alice" nil}}


   ;; explicity allow: identity-based - alice can do anything subscription based
   :explicit-allow-identity-based {:action "streams/CreateSubscription"
                                   :drn "domain.streams.subscription/my-sub"
                                   :resource-policy nil
                                   :identity-policies-map {"domain.security.role/alice" {:statements
                                                                                         [{:actions ["streams/*Subscription*"]
                                                                                           :resources ["domain.streams.subscription/*"]
                                                                                           :effect :ALLOW}]}}}

   :implicit-deny {:action "streams/CreateStream"
                   :drn "domain.streams.stream/my-stream"
                   :resource-policy nil
                   :identity-policies-map {"domain.security.role/alice" nil}}})


(deftest explicit-deny-resource-based
  (let [resp (core/evaluate-one (assoc (:explicit-deny-resource-based reqs) :map-return? true))]
    (is (= :DENY (:effect resp)))
    (is (= :EXPLICIT (:nature resp)))))

(deftest explicit-deny-identity-based
  (let [resp (core/evaluate-one (assoc (:explicit-deny-identity-based reqs) :map-return? true))]
    (is (= :DENY (:effect resp)))
    (is (= :EXPLICIT (:nature resp)))))

(deftest explicit-allow-resource-based
  (let [resp (core/evaluate-one (assoc (:explicit-allow-resource-based reqs) :map-return? true))]
    (is (= :ALLOW (:effect resp)))
    (is (= :EXPLICIT (:nature resp)))))

(deftest explicit-allow-identity-based
  (let [resp (core/evaluate-one (assoc (:explicit-allow-identity-based reqs) :map-return? true))]
    (is (= :ALLOW (:effect resp)))
    (is (= :EXPLICIT (:nature resp)))))

(deftest implicit-deny
  (let [resp (core/evaluate-one (assoc (:implicit-deny reqs) :map-return? true))]
    (is (= :DENY (:effect resp)))
    (is (= :IMPLICIT (:nature resp)))))
