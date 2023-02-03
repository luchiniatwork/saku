(ns saku.resolvers
  (:require [camel-snake-kebab.core :as csk]
            [com.walmartlabs.lacinia.schema :refer [tag-with-type]]
            [saku.core :as core]
            [saku.dal :as dal]
            [system-lacinia.resolvers-util :as util]))


(def ^:private statement-map
  [[:statement/actions :actionIds]
   [:statement/effect :effect #(-> % :effect csk/->SCREAMING_SNAKE_CASE_KEYWORD)]
   [:statement/identities :identities]
   [:statement/resources :resources]])

(def ^:private policy-map
  [[:policy/drn :drn]
   [:policy/statements :statements #(->> % (mapv (partial util/transform-ab statement-map)))]])


(def ^:private statement-input-map
  [[:actionIds :statement/actions]
   [:effect :statement/effect (fn [x] [:effect (csk/->kebab-case-keyword x)])]
   [:identities :statement/identities]
   [:resources :statement/resources]])

(def ^:private policy-input-map
  [[:drn :policy/drn]
   [:statements :policy/statements #(->> % (mapv (partial util/transform-ab statement-input-map)))]])


(def ^:private evaluation-statement-map
  [[:statement/actions :actions]
   [:statement/effect :effect #(-> % :effect csk/->SCREAMING_SNAKE_CASE_KEYWORD)]
   [:statement/identities :identities]
   [:statement/resources :resources]])

(def ^:private evaluation-policy-map
  [[:policy/drn :drn]
   [:policy/statements :statements #(->> % (mapv (partial util/transform-ab evaluation-statement-map)))]])


(defn get-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/get-policies (dal/db db-conn))
         (mapv (partial util/transform-ab policy-map))
         (mapv #(cond
                  (some :identities (:statements %))
                  (tag-with-type % :ResourcePolicyDocument)
                  (some :resources (:statements %))
                  (tag-with-type % :IdentityPolicyDocument))))))


(defn get-resource-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/get-resource-policies (dal/db db-conn))
         (mapv (partial util/transform-ab policy-map))
         (mapv #(tag-with-type % :ResourcePolicyDocument)))))


(defn get-identity-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/get-identity-policies (dal/db db-conn))
         (mapv (partial util/transform-ab policy-map))
         (mapv #(tag-with-type % :IdentityPolicyDocument)))))


(defn upsert-resource-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:policies args)
         (mapv (partial util/transform-ab policy-input-map))
         (dal/upsert-resource-policies db-conn)
         (mapv (partial util/transform-ab policy-map)))))


(defn upsert-identity-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:policies args)
         (mapv (partial util/transform-ab policy-input-map))
         (dal/upsert-identity-policies db-conn)
         (mapv (partial util/transform-ab policy-map)))))


(defn retract-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/retract-policies db-conn))))


(defn evaluate-one [{:keys [db-conn] :as opts}]
  (fn [ctx {:keys [drn identities actionId]} obj]
    (let [db (dal/db db-conn)
          resource-policy (->> [drn]
                               (dal/get-resource-policies db)
                               first
                               (util/transform-ab evaluation-policy-map))
          identity-policies (->> identities
                                 (dal/get-identity-policies db)
                                 (map (partial util/transform-ab evaluation-policy-map)))
          identity-policies-map (->> identities
                                     (reduce (fn [a i]
                                               (assoc a i (some #(when (= i (:drn %)) %)
                                                                identity-policies)))
                                             {}))]
      (core/evaluate-one {:drn drn
                          :action actionId
                          :resource-policy resource-policy
                          :identity-policies-map identity-policies-map}))))


(defn evaluate-many [{:keys [db-conn] :as opts}]
  (fn [ctx {:keys [drns identities actionId]} obj]
    (let [db (dal/db db-conn)
          resource-policies (->> drns
                                 (dal/get-resource-policies db)
                                 (map (partial util/transform-ab evaluation-policy-map)))
          identity-policies (->> identities
                                 (dal/get-identity-policies db)
                                 (map (partial util/transform-ab evaluation-policy-map)))
          identity-policies-map (->> identities
                                     (reduce (fn [a i]
                                               (assoc a i (some #(when (= i (:drn %)) %)
                                                                identity-policies)))
                                             {}))]
      (println "AQUIIIIII")
      (clojure.pprint/pprint (->> drns
                                  (reduce (fn [c drn]
                                            (let [resource-policy (some #(when (= drn (:drn %) %))
                                                                        resource-policies)]
                                              (conj c [{:drn drn
                                                        :result (core/evaluate-one {:drn drn
                                                                                    :action actionId
                                                                                    :resource-policy resource-policy
                                                                                    :identity-policies-map identity-policies-map})}])))
                                          [])))
      (->> drns
           (reduce (fn [c drn]
                     (let [resource-policy (some #(when (= drn (:drn %) %))
                                                 resource-policies)]
                       (conj c {:drn drn
                                :result (core/evaluate-one {:drn drn
                                                            :action actionId
                                                            :resource-policy resource-policy
                                                            :identity-policies-map identity-policies-map})})))
                   [])))))
