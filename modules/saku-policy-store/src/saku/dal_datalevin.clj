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


(defn ^:private impl-get-*-policies [db drns statement-attr]
  (apply d/q
    (cond-> {:find '[[(pull ?p pattern) ...]]
             :in   '[$ pattern [?drns ...]]
             :where
             '[[?p :policy/drn ?drns]
               [?p :policy/statements ?s]
               [?s ?statement-attr]]}
      statement-attr (->
                       (update :in conj '?statement-attr)
                       (update :where conj '[?s ?statement-attr])))
    (cond-> [db pattern drns]
      statement-attr (conj statement-attr))))


(defmethod interface/db :datalevin [{:keys [db-conn]}]
  (d/db db-conn))


(defmethod interface/get-policies :datalevin [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(interface/db obj) db-or-drns])]
    (impl-get-policies db' drns')))


(defmethod interface/get-resource-policies :datalevin [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(interface/db obj) db-or-drns])]
    (impl-get-*-policies db' drns' :statement/identities)))


(defmethod interface/get-identity-policies :datalevin [obj db-or-drns & [drns]]
  (let [[db' drns'] (if drns [db-or-drns drns] [(interface/db obj) db-or-drns])]
    (impl-get-*-policies db' drns' :statement/resources)))


(defn upsert-*-policies
  [db-conn policies]
  (let [tx-data (into []
                  (mapcat (fn [{:keys [policy/drn] :as policy}]
                            [[:db/retract [:policy/drn drn] :policy/statements]
                             policy]))
                  policies)]
    (d/transact! db-conn tx-data)))

(defmethod interface/upsert-resource-policies :datalevin [{:keys [db-conn] :as obj} policies]
  (schemas/assert* schemas/resource-policies policies)
  (let [{:keys [db-after]} (upsert-*-policies db-conn policies)]
    (interface/get-resource-policies obj db-after (map :policy/drn policies))))


(defmethod interface/upsert-identity-policies :datalevin [{:keys [db-conn] :as obj} policies]
  (schemas/assert* schemas/identity-policies policies)
  (let [{:keys [db-after]} (upsert-*-policies db-conn policies)]
    (interface/get-identity-policies obj db-after (map :policy/drn policies))))


(defn tx-fn-retract-statements-from-policy
  [db {:keys [policy/drn statement-ids]}]
  (let [eids-to-retract (d/q '[:find ?s
                               :in $ ?policy [?statement-id ...]
                               :where
                               [?policy :policy/statements ?s]
                               [?s :statement/sid ?statement-id]]
                          db [:policy/drn drn] statement-ids)]
    (map (fn [[eid]] [:db/retractEntity eid]) eids-to-retract)))

(defn add-statements*
  [db-conn statements-input]
  (let [{:keys [policy/drn policy/statements]} statements-input
        retract-sids (into [] (keep :statement/sid) statements)
        retract-tx (when (seq retract-sids)
                     [:db.fn/call tx-fn-retract-statements-from-policy
                      {:policy/drn    drn
                       :statement-ids retract-sids}])
        tx-data (into (cond-> [] retract-tx (conj retract-tx))
                  (map (fn [statement]
                         {:policy/drn        drn
                          :policy/statements statement}))
                  statements)]
    (d/transact! db-conn tx-data)))


(defmethod interface/add-identity-statements :datalevin [{:keys [db-conn] :as obj} add-statements-input]
  (schemas/assert* schemas/identity-policy add-statements-input)
  (let [{:keys [db-after]} (add-statements* db-conn add-statements-input)]
    (interface/get-identity-policies obj db-after [(:policy/drn add-statements-input)])))


(defmethod interface/add-resource-statements :datalevin [{:keys [db-conn] :as obj} add-statements-input]
  (schemas/assert* schemas/resource-policy add-statements-input)
  (let [{:keys [db-after]} (add-statements* db-conn add-statements-input)]
    (interface/get-resource-policies obj db-after [(:policy/drn add-statements-input)])))

(defmethod interface/retract-statements :datalevin [{:keys [db-conn] :as obj} retract-statements-input]
  (schemas/assert* [:map
                    [:policy/drn :string]
                    [:statement-ids [:sequential :string]]] retract-statements-input)
  (let [{:keys [db-after]} (d/transact! db-conn [[:db.fn/call tx-fn-retract-statements-from-policy retract-statements-input]])]
    (impl-get-*-policies db-after [(:policy/drn retract-statements-input)] nil)))


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
