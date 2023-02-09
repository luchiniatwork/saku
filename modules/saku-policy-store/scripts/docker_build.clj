(ns docker-build
  (:require [docker-settings :as settings]
            [clojure.java.shell :refer [sh]]))

(defn -main [& args]
  (try
    (let [tags (settings/tags)
          _ (println "Tags:" tags)
          tags-cmd (map (fn [x] ["-t" x]) tags)
          cmd (concat
               ["docker" "buildx" "build"]
               (flatten tags-cmd)
               ["."])
          _ (apply println cmd)
          {:keys [exit out err]} (apply sh cmd)]
      (println out)
      (when (not= 0 exit)
        (throw (ex-info "Error running docker build" {:cause err}))))
    (System/exit 0)
    (catch Exception e
      (println e)
      (System/exit 1))))
