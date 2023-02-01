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

RUN clojure -M:gen-sdl > resources/graphql.sdl

# RUN clojure -M:build
# RUN mv /usr/src/app/target/app*-standalone.jar target/app.jar

##################################################
## Server Proper
##################################################
# FROM --platform=linux/x86_64 openjdk:17-alpine as server
FROM --platform=linux/x86_64 clojure:openjdk-17-tools-deps-1.10.3.933-alpine as server
WORKDIR /usr/src/app

# Can be overridden with `docker run --env ENV=staging` for instance
ENV ENV=prod

# Build
# COPY --from=builder /usr/src/app/target/app.jar ./
COPY --from=builder /usr/src/app/. ./
COPY --from=builder /root/.m2 /root/.m2

# Proper entrypoint
# CMD java -cp app.jar clojure.main -m clojure.run.exec "-X:run"
CMD clojure -J-Xmx1G -X:run

##################################################
## Test image
##################################################
FROM --platform=linux/x86_64 openjdk:17-alpine as test
WORKDIR /usr/src/app

RUN apk update
RUN apk add curl
RUN apk add unzip
RUN curl -sSL https://rover.apollo.dev/nix/v0.4.1 | sh

# Can be overridden with `docker run --env PORT=3000` for instance
ENV ENV=prod
ENV APOLLO_KEY="APOLLO_KEY must be set for Apollo"
ENV SERVICE_NAME="SERVICE_NAME must be set for Apollo"
ENV ROUTING_URL="ROUTING_URL must be set for Apollo"

# Copy built file
#COPY --from=builder /usr/src/app/target/app.jar ./
COPY --from=builder /usr/src/app/resources/graphql.sdl graphql.sdl

# Copy docker-scripts
COPY --from=builder /usr/src/app/docker-scripts/* ./

# Extract SDL
#RUN unzip -d . app.jar graphql.sdl
