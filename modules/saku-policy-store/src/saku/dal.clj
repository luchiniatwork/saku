(ns saku.dal
  (:require [datalevin.core :as d]
            [saku.schemas :as schemas]))


(def ^:private pattern '[* {:policy/statements [* {:statement/effect [*]}]}])


(defn db [conn]
  (d/db conn))


(defn get-policies [db drns]
  (d/q '[:find [(pull ?p pattern) ...]
         :in $ pattern [?drns ...]
         :where
         [?p :policy/drn ?drns]]
       db
       pattern
       drns))


(defn get-resource-policies [db drns]
  (d/q '[:find [(pull ?p pattern) ...]
         :in $ pattern [?drns ...]
         :where
         [?p :policy/drn ?drns]
         [?p :policy/statements ?s]
         [?s :statement/identities]]
       db
       pattern
       drns))


(defn get-identity-policies [db drns]
  (d/q '[:find [(pull ?p pattern) ...]
         :in $ pattern [?drns ...]
         :where
         [?p :policy/drn ?drns]
         [?p :policy/statements ?s]
         [?s :statement/resources]]
       db
       pattern
       drns))


(defn upsert-resource-policies [conn policies]
  (schemas/assert* schemas/resource-policies policies)
  (let [drns (map :policy/drn policies)
        tx (reduce (fn [c {:keys [policy/drn] :as policy}]
                     (conj c
                           [:db/retract [:policy/drn drn] :policy/statements]
                           policy))
                   [] policies)
        tx-report (d/transact! conn tx)]
    (-> tx-report
        :db-after
        (get-resource-policies drns))))


(defn upsert-identity-policies [conn policies]
  (schemas/assert* schemas/identity-policies policies)
  (let [drns (map :policy/drn policies)
        tx (reduce (fn [c {:keys [policy/drn] :as policy}]
                     (conj c
                           [:db/retract [:policy/drn drn] :policy/statements]
                           policy))
                   [] policies)
        tx-report (d/transact! conn tx)]
    (-> tx-report
        :db-after
        (get-identity-policies drns))))


(defn retract-policies [conn drns]
  (let [tx (reduce (fn [c drn]
                     (conj c
                           [:db/retractEntity [:policy/drn drn]]))
                   [] drns)
        tx-report (d/transact! conn tx)]
    (->> tx-report
         :tx-data
         (filter (fn [[_ a _]] (= :policy/drn a)))
         (map (fn [[_ _ v]] v))
         set)))
