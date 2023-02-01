#!/usr/bin/env sh

/root/.rover/bin/rover subgraph publish \
                       storie-ai@$ENV \
                       --name $SERVICE_NAME \
                       --schema graphql.sdl \
                       --routing-url $ROUTING_URL
