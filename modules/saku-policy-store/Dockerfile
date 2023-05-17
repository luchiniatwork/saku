##################################################
## Server Build
##################################################
FROM azul/zulu-openjdk:17-latest as builder

# Install clojure CLI & required deps
RUN apt-get update && apt-get install curl tar gzip -y
RUN curl -O https://download.clojure.org/install/linux-install-1.11.1.1273.sh && chmod +x linux-install-1.11.1.1273.sh && ./linux-install-1.11.1.1273.sh

WORKDIR /usr/src/app

# RUN apk update
# RUN apk add git

# Dependencies
COPY deps.edn .
RUN clojure -M -e "(System/exit 0)"
# RUN clojure -R:build -e "(System/exit 0)"

# Build
COPY . ./

# RUN clojure -M:gen-sdl > resources/graphql.sdl

RUN clojure -X:uberjar

##################################################
## Server Proper
##################################################
FROM azul/zulu-openjdk:17-latest as server
WORKDIR /usr/src/app

# Add required lmbd package
RUN apt-get update && apt-get install liblmdb-dev -y

# Can be overridden with `docker run --env ENV=staging` for instance
ENV ENV=prod

# Copy Build
COPY --from=builder /usr/src/app/target/saku-policy-store.jar ./

# Proper entrypoint
# CMD java -cp app.jar clojure.main -m clojure.run.exec "-X:run"
CMD java -cp saku-policy-store.jar --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED saku.main
