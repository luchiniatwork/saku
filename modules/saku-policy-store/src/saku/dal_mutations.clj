(ns saku.dal-mutations
  (:require [datalevin.core :as d]
            [saku.schemas :as schemas]))


(defn upsert-resource-policies [conn policies]
  (schemas/assert* schemas/resource-policies policies)
  (let [tx (reduce (fn [c {:keys [policy/drn] :as policy}]
                     (conj c
                           [:db/retract [:policy/drn drn] :policy/statements]
                           policy))
                   [] policies)]
    (d/transact! conn tx)))


(defn upsert-identity-policies [conn policies]
  (schemas/assert* schemas/identity-policies policies)
  (let [tx (reduce (fn [c {:keys [policy/drn] :as policy}]
                     (conj c
                           [:db/retract [:policy/drn drn] :policy/statements]
                           policy))
                   [] policies)]
    (d/transact! conn tx)))

(defn retract-policies [conn drns]
  (let [tx (reduce (fn [c drn]
                     (conj c
                           [:db/retractEntity [:policy/drn drn]]))
                   [] drns)]
    (d/transact! conn tx)))
