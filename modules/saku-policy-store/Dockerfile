##################################################
## Server Build
##################################################
FROM --platform=linux/x86_64 clojure:openjdk-17-tools-deps-1.10.3.933-alpine as builder
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
# FROM --platform=linux/x86_64 openjdk:17-alpine as server
FROM amazoncorretto:19-alpine3.17 as server
WORKDIR /usr/src/app

# Can be overridden with `docker run --env ENV=staging` for instance
ENV ENV=prod

# Copy Build
COPY --from=builder /usr/src/app/target/saku-policy-store.jar ./

# Proper entrypoint
# CMD java -cp app.jar clojure.main -m clojure.run.exec "-X:run"
CMD java -cp saku-policy-store.jar --add-opens=java.base/java.nio=ALL-UNNAMED --add-opens=java.base/sun.nio.ch=ALL-UNNAMED saku.main
