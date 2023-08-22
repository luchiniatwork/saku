(ns saku.dal-datalevin
  (:require
    [clojure.set :as sets]
    [datalevin.core :as d]
    [saku.dal-interface :as dal]
    [saku.dal-interface :as interface]))

(def ^:private pattern '[* {:policy/statements [* {:statement/effect [*]}]}])

(def policy-type->statement-key
  {"Resource" :statement/identities
   "Identity" :statement/resources})
(def statement-key->policy-type (sets/map-invert policy-type->statement-key))

(defn ^:private policies-by-type [db drns statement-attr]
  (reduce
    (fn [acc [statement-k policy]]
      (update acc (statement-key->policy-type statement-k) (fnil conj []) policy))
    {}
    (apply d/q
      {:find '[?statement-attr (pull ?p pattern)]
       :in   '[$ pattern [?drn ...] [?statement-attr ...]]
       :where
       '[[?p :policy/drn ?drn]
         [?p :policy/statements ?s]
         [?s ?statement-attr]]}
      [db
       pattern
       drns
       (if statement-attr
         [statement-attr]
         [:statement/resources
          :statement/identities])])))

(defmethod interface/db :datalevin [{:keys [db-conn]}]
  (d/db db-conn))

(defmethod interface/-get-policies :datalevin
  [{:keys [dal-obj db]} {:keys [policyType drns]}]
  (into []
    (comp (map val) cat)
    (policies-by-type (or db (dal/db dal-obj))
      drns
      (policy-type->statement-key policyType))))

(defmethod interface/-upsert-policies :datalevin
  [{:keys [dal-obj] :as obj} {:keys [policies policyType]}]
  (let [tx-data (into []
                  (mapcat (fn [{:keys [policy/drn] :as policy}]
                            [[:db/retract [:policy/drn drn] :policy/statements]
                             policy]))
                  policies)
        {:keys [db-after]} (d/transact! (:db-conn dal-obj) tx-data)]
    (interface/get-policies {:dal-obj dal-obj :db db-after}
      {:policyType policyType
       :drns       (map :policy/drn policies)})))

(defn tx-fn-retract-statements-from-policy
  [db {:keys [policy/drn statement-ids]}]
  (let [eids-to-retract (d/q '[:find ?s
                               :in $ ?policy [?statement-id ...]
                               :where
                               [?policy :policy/statements ?s]
                               [?s :statement/sid ?statement-id]]
                          db [:policy/drn drn] statement-ids)]
    (map (fn [[eid]] [:db/retractEntity eid]) eids-to-retract)))

(defmethod interface/-add-statements :datalevin [{:keys [dal-obj]} add-statements-input]
  (let [{:keys [policy policyType]} add-statements-input
        {:keys [policy/drn policy/statements]} policy
        retract-sids (into [] (keep :statement/sid) statements)
        retract-tx (when (seq retract-sids)
                     [:db.fn/call tx-fn-retract-statements-from-policy
                      {:policy/drn    drn
                       :statement-ids retract-sids}])
        tx-data (into (cond-> [] retract-tx (conj retract-tx))
                  (map (fn [statement]
                         {:policy/drn        drn
                          :policy/statements statement}))
                  statements)
        {:keys [db-after]} (d/transact! (:db-conn dal-obj) tx-data)]
    (interface/get-policies {:dal-obj dal-obj :db db-after}
      {:drns       [drn]
       :policyType policyType})))

(defmethod interface/-retract-statements :datalevin [{:keys [dal-obj]} retract-statements-input]
  (let [{:keys [db-after]} (d/transact! (:db-conn dal-obj) [[:db.fn/call tx-fn-retract-statements-from-policy retract-statements-input]])]
    (first (dal/get-policies {:dal-obj dal-obj :db db-after} {:drns [(:policy/drn retract-statements-input)]}))))

(defmethod interface/-retract-policies :datalevin [{:keys [dal-obj]} {:keys [drns]}]
  (let [tx (reduce (fn [c drn]
                     (conj c
                       [:db/retractEntity [:policy/drn drn]]))
             [] drns)
        tx-report (d/transact! (:db-conn dal-obj) tx)]
    {:retracted-drns (->> tx-report
                       :tx-data
                       (filter (fn [[_ a _]] (= :policy/drn a)))
                       (map (fn [[_ _ v]] v))
                       set)}))


(defn dal-obj [{:keys [db-conn]}]
  {:impl    :datalevin
   :db-conn db-conn})

(comment
  (def conn (d/get-conn "/tmp/datalevin/mydb"))
  (def ctx {:dal-obj (dal-obj {:db-conn conn})})

  (policies-by-type
    (d/db conn)
    ["user1"]
    nil)

  (dal/get-policies ctx
    {:drns ["user1"]})

  (dal/upsert-policies ctx
    {:policyType "Identity"
     :policies   [{:policyr/drn       "user1"
                   :policy/statements [{:statement/sid       "s1"
                                        :statement/effect    [:effect :allow]
                                        :statement/resources ["r1" "r2"]
                                        :statement/actions   ["a1"]}]}]})
  (dal/upsert-policies ctx
    {:policyType "Resource"
     :policies   [{:policy/drn        "resource1"
                   :policy/statements [{:statement/sid        "s1"
                                        :statement/effect     [:effect :allow]
                                        :statement/identities ["user1"]
                                        :statement/actions    ["a1"]}]}]})

  (dal/retract-policies ctx {:drns ["user1"]})

  (dal/retract-statements ctx {:policy/drn    "user1"
                               :statement-ids ["s1"]})
  )
