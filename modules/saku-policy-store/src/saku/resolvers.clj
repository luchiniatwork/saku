(ns saku.resolvers
  (:require [camel-snake-kebab.core :as csk]
            [com.walmartlabs.lacinia.schema :refer [tag-with-type]]
            [saku.core :as core]
            [saku.dal-interface :as dal]
            [saku.schemas :as schemas]
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

(def ^:private retract-statements-input-map
  [[:drn :policy/drn]
   [:statement-ids :statement-ids]])


(def ^:private evaluation-statement-map
  [[:statement/actions :actions]
   [:statement/effect :effect #(-> % :effect csk/->SCREAMING_SNAKE_CASE_KEYWORD)]
   [:statement/identities :identities]
   [:statement/resources :resources]])

(def ^:private evaluation-policy-map
  [[:policy/drn :drn]
   [:policy/statements :statements #(->> % (mapv (partial util/transform-ab evaluation-statement-map)))]])


(defn get-policies [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/get-policies dal-obj)
         (mapv (partial util/transform-ab policy-map))
         (mapv #(cond
                  (some :identities (:statements %))
                  (tag-with-type % :ResourcePolicyDocument)
                  (some :resources (:statements %))
                  (tag-with-type % :IdentityPolicyDocument))))))


(defn get-resource-policies [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/get-resource-policies dal-obj)
         (mapv (partial util/transform-ab policy-map))
         (mapv #(tag-with-type % :ResourcePolicyDocument)))))


(defn get-identity-policies [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/get-identity-policies dal-obj)
         (mapv (partial util/transform-ab policy-map))
         (mapv #(tag-with-type % :IdentityPolicyDocument)))))


(defn upsert-resource-policies [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> (:policies args)
         (mapv (partial util/transform-ab policy-input-map))
         (dal/upsert-resource-policies dal-obj)
         (mapv (partial util/transform-ab policy-map)))))


(defn upsert-identity-policies [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> (:policies args)
         (mapv (partial util/transform-ab policy-input-map))
         (dal/upsert-identity-policies dal-obj)
         (mapv (partial util/transform-ab policy-map)))))

(defn add-*-statements [dal-obj add-fn args]
  (->> args
    (util/transform-ab policy-input-map)
    (add-fn dal-obj)
    (mapv (partial util/transform-ab policy-map))))

(defn add-identity-statements [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (add-*-statements dal-obj dal/add-identity-statements args)))


(defn add-resource-statements [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (add-*-statements dal-obj dal/add-resource-statements args)))


(defn retract-statements [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> args
      (util/transform-ab retract-statements-input-map)
      (dal/retract-statements dal-obj)
      (mapv (partial util/transform-ab policy-map)))))


(defn retract-policies [{:keys [dal-obj] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal/retract-policies dal-obj))))


(defn evaluate-one [{:keys [dal-obj] :as opts}]
  (fn [ctx {:keys [drn identities actionId] :as args} obj]
    (schemas/assert* schemas/evaluate-one-args args)
    (let [db (dal/db dal-obj)
          resource-policy (->> [drn]
                               (dal/get-resource-policies dal-obj db)
                               first
                               (util/transform-ab evaluation-policy-map))
          identity-policies (->> identities
                                 (dal/get-identity-policies dal-obj db)
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


(defn evaluate-many [{:keys [dal-obj] :as opts}]
  (fn [ctx {:keys [drns identities actionId] :as args} obj]
    (schemas/assert* schemas/evaluate-many-args args)
    (let [db (dal/db dal-obj)
          resource-policies (->> drns
                                 (dal/get-resource-policies dal-obj db)
                                 (map (partial util/transform-ab evaluation-policy-map)))
          identity-policies (->> identities
                                 (dal/get-identity-policies dal-obj db)
                                 (map (partial util/transform-ab evaluation-policy-map)))
          identity-policies-map (->> identities
                                     (reduce (fn [a i]
                                               (assoc a i (some #(when (= i (:drn %)) %)
                                                                identity-policies)))
                                             {}))]
      (->> drns
           (reduce (fn [c drn]
                     (let [resource-policy (some #(when (= drn (:drn %)) %)
                                                 resource-policies)]
                       (conj c {:drn drn
                                :result (core/evaluate-one {:drn drn
                                                            :action actionId
                                                            :resource-policy resource-policy
                                                            :identity-policies-map identity-policies-map})})))
                   [])))))


(defn server-meta [{:keys [version] :as opts}]
  (fn [ctx args obj]
    {:environmentId (-> version :environment-id name)
     :version (-> version :version)}))
