(ns saku.resolvers
  (:require [camel-snake-kebab.core :as csk]
            [com.walmartlabs.lacinia.schema :as schema]
            ;;[kkiss.core :as kkiss]
            ;;[te.dal-queries :as dal]
            [system-lacinia.resolvers-util :as util]))


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
