(ns gen-sdl
  (:require [clojure.java.io :as io]
            [hodur-engine.core :as hodur]
            [hodur-lacinia-schema.core :as hodur-lacinia]))

(defn -main [& args]
  (-> "public/schemas/graphql.edn"
      io/resource
      hodur/init-path
      (hodur-lacinia/schema {:output :sdl})
      println))
