(ns saku.system.datalevin
  (:require [integrant.core :as ig]
            [taoensso.timbre :refer [debug info warn error fatal report] :as timbre]
            [datalevin.core :as d]))


(defn schema []
  {:effect {:db/valueType :db.type/keyword
            :db/unique :db.unique/identity}

   :policy/drn {:db/valueType :db.type/string
                :db/unique :db.unique/identity}

   :policy/statements {:db/isComponent true
                       :db/valueType :db.type/ref
                       :db/cardinality :db.cardinality/many}

   :statement/actions {:db/valueType :db.type/string
                       :db/cardinality :db.cardinality/many}

   :statement/effect {:db/valueType :db.type/ref
                      :db/cardinality :db.cardinality/one}

   :statement/identities {:db/valueType :db.type/string
                          :db/cardinality :db.cardinality/many}

   :statement/resources {:db/valueType :db.type/string
                         :db/cardinality :db.cardinality/many}

   :statement/sid {:db/valueType   :db.type/string
                   :db/cardinality :db.cardinality/one}})

(defmethod ig/init-key ::schema [_ _]
  (info {:msg "Initializing Dataleving Schema"})
  (schema))


(defn conn [url schema seed]
  (let [conn (d/get-conn url schema)]
    (d/transact! conn seed)
    conn))


(defmethod ig/init-key ::conn [_ {:keys [schema seed url]}]
  (info {:msg "Initializing Datalevin Connection"
         :url url})
  (conn url schema seed))


(defmethod ig/halt-key! ::conn [_ conn]
  (info {:msg "Closing Datalevin Connection"})
  (d/close conn))
