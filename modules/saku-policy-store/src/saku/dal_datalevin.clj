(ns saku.dal-datalevin
  (:require [datalevin.core :as d]
            [saku.dal-interface :as interface]
            [saku.schemas :as schemas]))


(def ^:private pattern '[* {:policy/statements [* {:statement/effect [*]}]}])


(defn ^:private impl-get-policies [db drns]
  (d/q '[:find [(pull ?p pattern) ...]
         :in $ pattern [?drns ...]
         :where
         [?p :policy/drn ?drns]]
       db
       pattern
       drns))


(defn ^:private impl-get-resource-policies [db drns]
  (d/q '[:find [(pull ?p pattern) ...]
         :in $ pattern [?drns ...]
         :where
         [?p :policy/drn ?drns]
         [?p :policy/statements ?s]
         [?s :statement/identities]]
       db
       pattern
       drns))


(defn ^:private impl-get-identity-policies [db drns]
  (d/q '[:find [(pull ?p pattern) ...]
         :in $ pattern [?drns ...]
         :where
         [?p :policy/drn ?drns]
         [?p :policy/statements ?s]
         [?s :statement/resources]]
       db
       pattern
       drns))


(defmethod interface/db :datalevin [{:keys [db-conn]}]
  (d/db db-conn))


(defmethod interface/get-policies :datalevin [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(interface/db obj) db-or-drns])]
    (impl-get-policies db' drns')))


(defmethod interface/get-resource-policies :datalevin [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(interface/db obj) db-or-drns])]
    (impl-get-resource-policies db' drns')))


(defmethod interface/get-identity-policies :datalevin [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(interface/db obj) db-or-drns])]
    (impl-get-identity-policies db' drns')))


(defmethod interface/upsert-resource-policies :datalevin [{:keys [db-conn] :as obj} policies]
  (schemas/assert* schemas/resource-policies policies)
  (let [drns (map :policy/drn policies)
        tx (reduce (fn [c {:keys [policy/drn] :as policy}]
                     (conj c
                           [:db/retract [:policy/drn drn] :policy/statements]
                           policy))
                   [] policies)
        {:keys [db-after]} (d/transact! db-conn tx)]
    (->> drns
         (interface/get-resource-policies obj db-after))))


(defmethod interface/upsert-identity-policies :datalevin [{:keys [db-conn] :as obj} policies]
  (schemas/assert* schemas/identity-policies policies)
  (let [drns (map :policy/drn policies)
        tx (reduce (fn [c {:keys [policy/drn] :as policy}]
                     (conj c
                           [:db/retract [:policy/drn drn] :policy/statements]
                           policy))
                   [] policies)
        {:keys [db-after]}(d/transact! db-conn tx)]
    (->> drns
         (interface/get-identity-policies obj db-after))))


(defmethod interface/retract-policies :datalevin [{:keys [db-conn]} drns]
  (let [tx (reduce (fn [c drn]
                     (conj c
                           [:db/retractEntity [:policy/drn drn]]))
                   [] drns)
        tx-report (d/transact! db-conn tx)]
    (->> tx-report
         :tx-data
         (filter (fn [[_ a _]] (= :policy/drn a)))
         (map (fn [[_ _ v]] v))
         set)))


(defn dal-obj [{:keys [db-conn]}]
  {:impl :datalevin
   :db-conn db-conn})
