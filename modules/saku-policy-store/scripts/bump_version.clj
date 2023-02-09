(ns bump-version
  (:require [orzo.core :as orzo]))

(defn -main [& args]
  (try
    (println (-> (orzo/read-file "resources/version.txt")
                 (orzo/calver "YY.MM.CC")
                 (orzo/overwrite-file "README.md" #"saku-policy-store:\d+.\d+.\d+"
                                      #(str "saku-policy-store" %))
                 (orzo/save-file "resources/version.txt")
                 (orzo/stage)))
    (System/exit 0)
    (catch Exception e
      (println e)
      (System/exit 1))))
