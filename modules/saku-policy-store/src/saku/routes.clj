(ns saku.routes
  (:require
    [camel-snake-kebab.core :as csk]
    [clojure.set :as sets]
    [malli.core :as m]
    [malli.transform :as mt]
    [muuntaja.core :as muuntaja]
    [reitit.coercion.malli :as malli]
    [reitit.ring.coercion :as coercion]
    [reitit.ring.middleware.exception :as exception]
    [reitit.ring.middleware.muuntaja :as muuntaja.middleware]
    [reitit.ring.middleware.parameters :as parameters]
    [saku.core :as core]
    [saku.dal-interface :as dal]
    [saku.schemas :as schemas]
    [taoensso.timbre :as log]))

(defn health-handler
  [_]
  {:status 200
   :body   {}})

(defn wrap-transformed
  [handler {:keys [input-schema output-schema]}]
  (fn [request]
    (let [{:keys [parameters]} request
          {:keys [body]} parameters
          input-data (m/decode input-schema body
                       (schemas/rename-keys-transformer {:rename-key :output-k}))
          {:keys [body] :as response} (handler (assoc request :saku/input-data input-data))
          output-data (when body
                        (m/decode output-schema body
                          (mt/transformer
                            (schemas/rename-keys-transformer {:rename-key :output-k})
                            (mt/strip-extra-keys-transformer))))]
      (cond-> response
        output-data (assoc :body output-data)))))

(defn upsert-policies-handler
  [request]
  (let [{:saku/keys [input-data ctx]} request
        result (dal/upsert-policies {:dal-obj (:dal-obj ctx)} input-data)]
    {:status 200
     :body   {:policyType (:policyType input-data)
              :policies   result}}))

(defn retract-policies-handler
  [request]
  (let [{:saku/keys [input-data ctx]} request
        result (dal/retract-policies {:dal-obj (:dal-obj ctx)} input-data)]
    {:status 200
     :body   result}))

(defn add-statements-handler
  [request]
  (let [{:saku/keys [input-data ctx]} request
        result (dal/add-statements {:dal-obj (:dal-obj ctx)} input-data)]
    {:status 200
     :body   {:policyType (:policyType input-data)
              :policies   result}}))

(defn retract-statements-handler
  [request]
  (let [{:saku/keys [input-data ctx]} request
        result (dal/retract-statements {:dal-obj (:dal-obj ctx)} input-data)]
    {:status 200
     :body   result}))

(defn describe-policies-handler
  [request]
  (let [{:saku/keys [input-data ctx]} request
        result (dal/get-policies {:dal-obj (:dal-obj ctx)} input-data)]
    {:status 200
     :body   {:policies result}}))

(defn ->evaluate-policy
  [policy]
  (-> policy
    (update :policy/statements (fn [statements]
                                 (mapv (fn [statement]
                                         (-> statement
                                           (sets/rename-keys {:statement/actions    :actions
                                                              :statement/effect     :effect
                                                              :statement/identities :identities
                                                              :statement/resources  :resources})
                                           (update :effect #(-> % :effect csk/->SCREAMING_SNAKE_CASE_KEYWORD))))
                                   statements)))
    (sets/rename-keys {:policy/drn        :drn
                       :policy/statements :statements})))

(defn evaluate-one [{:keys [dal-obj]} evaluate-one-input]
  (let [{:keys [drn identities actionId]} evaluate-one-input
        db (dal/db dal-obj)
        ctx {:dal-obj dal-obj :db db}
        resource-policy (-> (dal/get-policies ctx {:drns [drn] :policyType "Resource"})
                          first
                          ->evaluate-policy)
        identity-policies (map ->evaluate-policy (dal/get-policies ctx {:drns identities :policyType "Identity"}))
        identity-policies-map (into {}
                                (map (fn [identity-drn]
                                       [identity-drn (some #(when (= identity-drn (:drn %)) %)
                                                       identity-policies)]))
                                identities)]
    (core/evaluate-one {:drn                   drn
                        :action                actionId
                        :resource-policy       resource-policy
                        :identity-policies-map identity-policies-map})))


(defn evaluate-many [{:keys [dal-obj]} evaluate-many-input]
  (let [{:keys [drns identities actionId]} evaluate-many-input
        db (dal/db dal-obj)
        ctx {:dal-obj dal-obj :db db}
        resource-policies (mapv ->evaluate-policy (dal/get-policies ctx {:drns drns :policyType "Resource"}))
        identity-policies (mapv ->evaluate-policy (dal/get-policies ctx {:drns drns :policyType "Identity"}))
        identity-policies-map (->> identities
                                (reduce (fn [a i]
                                          (assoc a i (some #(when (= i (:drn %)) %)
                                                       identity-policies)))
                                  {}))]
    (->> drns
      (reduce (fn [c drn]
                (let [resource-policy (some #(when (= drn (:drn %)) %)
                                        resource-policies)]
                  (conj c {:drn    drn
                           :result (core/evaluate-one {:drn                   drn
                                                       :action                actionId
                                                       :resource-policy       resource-policy
                                                       :identity-policies-map identity-policies-map})})))
        []))))

(defn evaluate-one-handler
  [request]
  (let [{:saku/keys [ctx]
         :keys      [parameters]} request
        result (evaluate-one {:dal-obj (:dal-obj ctx)} (:body parameters))]
    {:status 200
     :body   result}))

(defn evaluate-many-handler
  [request]
  (let [{:saku/keys [ctx]
         :keys      [parameters]} request
        result (evaluate-many {:dal-obj (:dal-obj ctx)} (:body parameters))]
    {:status 200
     :body   result}))

(def wrap-ctx
  {:name        ::ctx
   :description "Add ctx to request map."
   :wrap        (fn [handler ctx]
                  (fn [request]
                    (handler (assoc request :saku/ctx ctx))))})

(def wrap-request-id
  {:name        ::request-id
   :description "Add request-id to the log mdc"
   :wrap        (fn [handler]
                  (fn [request]
                    (log/with-context+ {:request_id (random-uuid)}
                      (handler request))))})

(defn exception-handler [message status exception request]
  (when (>= status 500)
    ;; You can optionally use this to report error to an external service
    (log/error exception))
  {:status status
   :body   {:message   message
            :exception (.getName (.getClass exception))}})

(def wrap-exception
  (exception/create-exception-middleware
    (merge
      exception/default-handlers
      {::exception/default #(exception-handler "default" 500 %1 %2)
       ;; print stack-traces for all exceptions
       ::exception/wrap    (fn [handler e request]
                             (handler e request))})))

(defn route-data
  [ctx]
  {:coercion   malli/coercion
   :muuntaja   (-> muuntaja/default-options
                 (update :formats select-keys ["application/json"])
                 muuntaja/create)
   :middleware [wrap-request-id
                [wrap-ctx ctx]
                ;; query-params & form-params
                parameters/parameters-middleware
                ;; content-negotiation
                muuntaja.middleware/format-negotiate-middleware
                ;; encoding response body
                muuntaja.middleware/format-response-middleware
                ;; exception handling
                coercion/coerce-exceptions-middleware
                ;; decoding request body
                muuntaja.middleware/format-request-middleware
                ;; coercing response bodys
                coercion/coerce-response-middleware
                ;; coercing request parameters
                coercion/coerce-request-middleware
                ;; exception handling
                wrap-exception]})

(defn api-routes
  [{:saku.system.dal/keys [dal-obj]}]
  [["/api" (route-data {:dal-obj dal-obj})
    ["/health" {:get health-handler}]

    ;; Mutation
    ["/UpsertPolicies" {:middleware [[wrap-transformed {:input-schema  schemas/UpsertPoliciesInput
                                                        :output-schema schemas/UpsertPoliciesOutput}]]
                        :post       upsert-policies-handler
                        :parameters {:body schemas/UpsertPoliciesInput}}]
    ["/RetractPolicies" {:middleware [[wrap-transformed {:input-schema  schemas/RetractPoliciesInput
                                                         :output-schema schemas/RetractPoliciesOutput}]]
                         :post       retract-policies-handler
                         :parameters {:body schemas/RetractPoliciesInput}}]
    ["/AddStatements" {:middleware [[wrap-transformed {:input-schema  schemas/AddStatementsInput
                                                       :output-schema schemas/AddStatementsOutput}]]
                       :post       add-statements-handler
                       :parameters {:body schemas/AddStatementsInput}}]
    ["/RetractStatements" {:middleware [[wrap-transformed {:input-schema  schemas/RetractStatementsInput
                                                           :output-schema schemas/RetractStatementsOutput}]]
                           :post       retract-statements-handler
                           :parameters {:body schemas/RetractStatementsInput}}]
    ;; Query
    ["/DescribePolicies" {:middleware [[wrap-transformed {:input-schema  schemas/DescribePoliciesInput
                                                          :output-schema schemas/DescribePoliciesOutput}]]
                          :post       describe-policies-handler
                          :parameters {:body schemas/DescribePoliciesInput}}]

    ;; Policy evaluation
    ["/EvaluateOne" {:post       evaluate-one-handler
                     :parameters {:body schemas/EvaluateOneInput}}]
    ["/EvaluateMany" {:post       evaluate-many-handler
                      :parameters {:body schemas/EvaluateManyInput}}]]])
