(ns saku.resolvers
  (:require [camel-snake-kebab.core :as csk]
            [com.walmartlabs.lacinia.schema :as schema]
            [saku.dal :as dal]
            [com.walmartlabs.lacinia.schema :refer [tag-with-type]]
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
   [:identities :statement/identites]
   [:resources :statement/resources]])

(def ^:private policy-input-map
  [[:drn :policy/drn]
   [:statements :policy/statements #(->> % (mapv (partial util/transform-ab statement-input-map)))]])



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
