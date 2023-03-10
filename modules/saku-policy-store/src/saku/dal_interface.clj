(ns saku.dal-interface)

(defmulti db :impl)

(defmethod db :default [_]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))


(defmulti get-policies (fn [obj db-or-drns & _] (:impl obj)))

(defmethod get-policies :default [_ db-or-drns & _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))


(defmulti get-resource-policies (fn [obj db-or-drns & _] (:impl obj)))

(defmethod get-resource-policies :default [_ db-or-drns & _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))


(defmulti get-identity-policies (fn [obj db-or-drns & _] (:impl obj)))

(defmethod get-identity-policies :default [_ db-or-drns & _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))


(defmulti upsert-resource-policies (fn [obj policies] (:impl obj)))

(defmethod upsert-resource-policies :default [_ policies]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))


(defmulti upsert-identity-policies (fn [obj policies] (:impl obj)))

(defmethod upsert-identity-policies :default [_ policies]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))


(defmulti retract-policies (fn [obj drns] (:impl obj)))

(defmethod retract-policies :default [_ drns]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))
