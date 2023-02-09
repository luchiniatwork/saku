(ns docker-settings
  (:require [orzo.core :as orzo]
            [orzo.git :as git]))

(def repo "luchiniatwork/saku-policy-store")

(defn tags []
  (let [calver (-> (orzo/read-file "resources/version.txt")
                   (orzo/prepend "v"))
        sha (-> (git/sha)
                (orzo/prepend "sha-")
                (orzo/append (git/unclean-status "-dirty")))]
    [(str repo ":" calver)
     (str repo ":" sha)
     (str repo ":latest")]))
