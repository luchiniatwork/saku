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
    [kwill.logger :as log]))

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

(defn evaluate-many
  [{:keys [dal-obj]} evaluate-many-input]
  (let [{:keys [drns identities actionId]} evaluate-many-input
        db (dal/db dal-obj)
        ctx {:dal-obj dal-obj :db db}
        resource-policies (mapv ->evaluate-policy (dal/get-policies ctx {:drns drns :policyType "Resource"}))
        drn->resource-policy (into {} (map (juxt :drn identity)) resource-policies)
        identity-policies (mapv ->evaluate-policy (dal/get-policies ctx {:drns identities :policyType "Identity"}))
        drn->identity-policy (into {} (map (juxt :drn identity)) identity-policies)]
    (into []
      (map (fn [drn]
             (let [resource-policy (drn->resource-policy drn)
                   evaluate-argm {:drn                   drn
                                  :action                actionId
                                  :resource-policy       resource-policy
                                  :identity-policies-map drn->identity-policy}
                   result (core/evaluate-one evaluate-argm)]
               (log/info (assoc evaluate-argm
                           :result result
                           :msg "Evaluating effect..."))
               {:drn drn :result result})))
      drns)))

(defn evaluate-one-handler
  [request]
  (let [{:saku/keys [ctx]
         :keys      [parameters]} request
        many-result (evaluate-many {:dal-obj (:dal-obj ctx)}
                      (-> (:body parameters)
                        (update :drn vector)
                        (sets/rename-keys {:drn :drns})))
        result (-> many-result first :result)]
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
                      (log/info {:msg            "Received request"
                                 :uri            (:uri request)
                                 :request-method (:request-method request)
                                 :host           (get-in request [:headers "host"])
                                 :user-agent     (get-in request [:headers "user-agent"])})
                      (let [response (handler request)]
                        (log/info {:msg    "Finished request"
                                   :status (:status response)})
                        response))))})

(defn exception-handler [message status exception request]
  (when (>= status 500)
    ;; You can optionally use this to report error to an external service
    (log/error {:msg       "Encountered error while processing request."
                :throwable exception}))
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
  [["" (route-data {:dal-obj dal-obj})
    ["/api"
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
                       :parameters {:body schemas/EvaluateManyInput}}]]]])
