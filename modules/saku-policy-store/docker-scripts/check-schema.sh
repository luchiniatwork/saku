#!/usr/bin/env sh

/root/.rover/bin/rover subgraph check \
                       storie-ai@$ENV \
                       --name $SERVICE_NAME \
                       --schema graphql.sdl
