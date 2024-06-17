# HMPPS Non-associations API
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-non-associations-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-non-associations-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-non-associations-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-non-associations-api)
[![Runbook](https://img.shields.io/badge/runbook-view-172B4D.svg?logo=confluence)](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/1739325587/DPS+Runbook)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://non-associations-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/hmpps-non-associations-api/main/async-api.yml)

**Non-associations API to own the non-associations data for prisoners**

## Running locally against dev/T3 services

This is straight-forward as authentication is delegated down to the calling services in `dev` environment.
Environment variables to be set are as follows:

```
API_BASE_URL_OAUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
NON_ASSOCIATIONS_API_CLIENT_ID=[choose a suitable hmpps-auth client]
NON_ASSOCIATIONS_API_CLIENT_SECRET=
```

Start the database and other required services via docker-compose with:

```shell
docker compose -f docker-compose-local.yml up
```

Then run the API.

## Connecting to AWS resources from a local port

There are custom gradle tasks that make it easier to connect to AWS resources (RDS and ElastiCache Redis)
in Cloud Platform from a local port:

```shell
./gradlew portForwardRDS
# and
./gradlew portForwardRedis
```

These could be useful to, for instance, clear out a development database or edit data live.

They require `kubectl` to already be set up to access the kubernetes cluster;
essentially these tasks are just convenience wrappers.

Both accept the `--environment` argument to select between `dev`, `preprod` and `prod` namespaces
or prompt for user input when run.

Both also accept the `--port` argument to choose a different local port, other than the resource’s default.

## Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)
