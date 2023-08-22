(ns saku.test-utils
  (:require [clojure.java.io :as io]
            [clojure.walk :as walk]
            [datalevin.core :as d]
            [saku.dal-datalevin :as dal-impl]
            [saku.system.datalevin :as db-sys]
            [saku.system.seed :as seed-sys]))

(defn create-temp-dir []
  (Files/createTempDirectory "saku-policy-store"
                             (make-array FileAttribute 0)))

(defn delete-files
  "Recursively delete file"
  [& fs]
  (when-let [f (first fs)]
    (if-let [cs (seq (.listFiles (io/file f)))]
      (recur (concat cs fs))
      (do (io/delete-file f)
          (recur (rest fs))))))

(defmacro with-temp-dir [spec & body]
  `(let [dir#    (create-temp-dir)]
     (try
       (let [~(first spec) dir#] ~@body)
       (finally
         (delete-files (.toFile dir#))))))

(defmacro with-conn [spec & body]
  `(with-temp-dir [dir#]
     (let [conn# (db-sys/conn (.toString dir#) (db-sys/schema) (seed-sys/seed))]
       (try
         (let [~(first spec) conn#]
           ~@body)
         (finally
           (d/close conn#))))))

(defmacro with-dal-ctx [[binding opts] & body]
  `(with-conn [conn#]
     (let [dal-obj# (dal-impl/dal-obj {:db-conn conn#})
           ~binding {:dal-obj dal-obj#}]
       ~@body)))


(defn sanitize-from-db [entry]
  (walk/postwalk (fn [x]
                   (cond
                     (and (map? x) (:effect x))
                     [:effect (:effect x)]

                     (and (map? x) (not (:effect x)))
                     (dissoc x :db/id)

                     :else
                     x))
                 entry))
