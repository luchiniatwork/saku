(ns saku.dal-queries
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
