(ns saku.resolvers
  (:require [camel-snake-kebab.core :as csk]
            [com.walmartlabs.lacinia.schema :as schema]
            ;;[kkiss.core :as kkiss]
            ;;[te.dal-queries :as dal]
            [saku.dal-mutations :as dal-m]
            [saku.dal-queries :as dal-q]
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


(defn get-policies [{:keys [db-conn] :as opts}]
  (fn [ctx args obj]
    (->> (:drns args)
         (dal-q/get-policies (dal-q/db db-conn))
         (mapv (partial util/transform-ab policy-map))
         (mapv #(cond
                  (some :identities (:statements %))
                  (tag-with-type % :ResourcePolicyDocument)
                  (some :resources (:statements %))
                  (tag-with-type % :IdentityPolicyDocument))))))



#_(def ^:private inspiration-map
    [[:inspiration/id :id]
     [:inspiration/source :source #(-> % :db/ident name csk/->SCREAMING_SNAKE_CASE_KEYWORD)]
     [:inspiration/input :input]
     [:inspiration/payload :payload]])

#_(defn ctx-user-id [ctx]
    (some-> ctx :request :headers (get "x-user-id")))

#_(defn parent-user-id [obj]
    (some-> obj :id))





#_(defn get-all-inspirations [{:keys [db-conn] :as opts}]
    (fn [ctx args obj]
      (mapv (partial util/transform-ab inspiration-map)
            (dal/get-all-inspirations db-conn (parent-user-id obj)))))

#_(defn get-inspirations [{:keys [db-conn] :as opts}]
    (fn [ctx {first-x :first
              after :after
              last-x :last
              before :before
              order-by :orderBy
              :as args}
         obj]
      (let [inspirations (->> {:first-x first-x
                               :after after
                               :last-x last-x
                               :before before
                               :order-by order-by}
                              (dal/get-inspirations db-conn (parent-user-id obj)))
            nodes (->> inspirations
                       :edges
                       (map (partial util/transform-ab inspiration-map)))
            edges (->> inspirations
                       :edges
                       (map (fn [s] {:cursor (:cursor s)
                                     :node (util/transform-ab inspiration-map s)})))]
        {:pageInfo {:startCursor (-> inspirations :page-info :start-cursor)
                    :endCursor (-> inspirations :page-info :end-cursor)
                    :hasNextPage (-> inspirations :page-info :next-page?)
                    :hasPreviousPage (-> inspirations :page-info :prev-page?)}
         :edges edges
         :nodes nodes})))

#_(defn get-inspiration-entity [{:keys [db-conn] :as opts}]
    (fn [ctx _ reps]
      (->> reps
           (map :id)
           (dal/get-inspirations-by-ids db-conn (ctx-user-id ctx))
           (map (partial util/transform-ab inspiration-map))
           (map #(schema/tag-with-type % :Inspiration)))))
