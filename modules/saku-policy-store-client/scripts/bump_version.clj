(ns bump-version
  (:require [orzo.core :as orzo]))

(defn -main [& args]
  (try
    (println (-> (orzo/read-file "version.txt")
                 (orzo/calver "YY.MM.CC")
                 (orzo/overwrite-file "README.md" #":mvn/version \"\d+.\d+.\d+\""
                                      #(str ":mvn/version \"" % "\""))
                 (orzo/overwrite-file "package.json" #"\"version\": \"\d+.\d+.\d+\""
                                      #(str "\"version\": \"" % "\""))
                 (orzo/overwrite-file "pom.xml"
                                      #"<artifactId>saku-policy-store-client</artifactId>\n(\s*)<version>.+</version>"
                                      #(str "<artifactId>saku-policy-store-client</artifactId>\n$1"
                                            "<version>" % "</version>"))
                 (orzo/save-file "version.txt")))
    (System/exit 0)
    (catch Exception e
      (println e)
      (System/exit 1))))
