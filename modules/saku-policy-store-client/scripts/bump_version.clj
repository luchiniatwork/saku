(ns bump-version
  (:require [orzo.core :as orzo]))

(defn -main [& args]
  (try
    (println (-> (orzo/read-file "version.txt")
                 (orzo/calver "YY.MM.CC")
                 (orzo/overwrite-file "package.json" #"\"version\": \"\d+.\d+.\d+\""
                                      #(str "\"version\": \"" % "\""))
                 (orzo/save-file "version.txt")))
    (System/exit 0)
    (catch Exception e
      (println e)
      (System/exit 1))))
