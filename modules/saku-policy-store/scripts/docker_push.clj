(ns docker-push
  (:require [docker-settings :as settings]
            [clojure.java.shell :refer [sh]]))

(defn -main [& args]
  (try
    (let [cmd ["docker" "image" "push" "--all-tags" settings/repo]]
      (apply println cmd)
      (let [{:keys [exit out err]} (apply sh cmd)]
        (println out)
        (when (not= 0 exit)
          (throw (ex-info "Error running docker build" {:cause err})))))

    (System/exit 0)
    (catch Exception e
      (println e)
      (System/exit 1))))
