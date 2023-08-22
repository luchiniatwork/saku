(ns saku.dal-interface)

(defmulti db :impl)

(defmethod db :default [_]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))

(defn- interface-dispatch
  [ctx _argm]
  (-> ctx :dal-obj :impl))

(defmulti -get-policies interface-dispatch)

(defmethod -get-policies :default [_ _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))

(defn get-policies
  [ctx argm]
  (-get-policies ctx argm))

(defmulti -upsert-policies interface-dispatch)

(defmethod -upsert-policies :default [_ _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))

(defn upsert-policies
  [ctx argm]
  (-upsert-policies ctx argm))

(defmulti -add-statements interface-dispatch)

(defmethod -add-statements :default [_ _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))

(defn add-statements
  [ctx argm]
  (-add-statements ctx argm))

(defmulti -retract-statements interface-dispatch)

(defmethod -retract-statements :default [_ _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))

(defn retract-statements
  [ctx argm]
  (-retract-statements ctx argm))

(defmulti -retract-policies interface-dispatch)

(defmethod -retract-policies :default [_ _]
  (throw (ex-info "Not Implemented" {:anomaly/category ::not-implemented})))

(defn retract-policies
  [ctx argm]
  (-retract-policies ctx argm))
