(ns bump-version
  (:require [orzo.core :as orzo]))

(defn -main [& args]
  (try
    (println (-> (orzo/read-file "resources/version.txt")
                 (orzo/calver "YY.MM.CC")
                 (orzo/overwrite-file "README.md" #"saku-policy-store:v\d+.\d+.\d+"
                                      #(str "saku-policy-store:v" %))
                 (orzo/overwrite-file "package.json" #"\"version\": \"\d+.\d+.\d+\""
                                      #(str "\"version\": \"" % "\""))
                 (orzo/overwrite-file "pom.xml"
                                      #"<artifactId>saku-policy-store</artifactId>\n(\s*)<version>.+</version>"
                                      #(str "<artifactId>saku-policy-store</artifactId>\n$1"
                                            "<version>" % "</version>"))
                 (orzo/save-file "resources/version.txt")
                 (orzo/stage)))
    (System/exit 0)
    (catch Exception e
      (println e)
      (System/exit 1))))
