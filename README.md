# HMPPS Non-associations API
[![CircleCI](https://circleci.com/gh/ministryofjustice/hmpps-non-associations-api/tree/main.svg?style=svg)](https://circleci.com/gh/ministryofjustice/hmpps-non-associations-api)
[![Docker Repository on Quay](https://quay.io/repository/hmpps/hmpps-non-associations-api/status "Docker Repository on Quay")](https://quay.io/repository/hmpps/hmpps-non-associations-api)
[![Runbook](https://img.shields.io/badge/runbook-view-172B4D.svg?logo=confluence)](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/1739325587/DPS+Runbook)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://non-associations-api-dev.hmpps.service.justice.gov.uk/webjars/swagger-ui/index.html?configUrl=/v3/api-docs)

**Non-associations API to own the non-associations data for prisoners**

## Running locally against dev/T3 services

This is straight-forward as authentication is delegated down to the calling services in `dev` environment.
Environment variables to be set are as follows:

```
API_BASE_URL_OAUTH=https://sign-in-dev.hmpps.service.justice.gov.uk/auth
API_BASE_URL_PRISON=https://api-dev.prison.service.justice.gov.uk
INCENTIVES_API_CLIENT_ID=[choose a suitable hmpps-auth client]
INCENTIVES_API_CLIENT_SECRET=
```

Start the database and other required services via docker-compose with:

`$ docker-compose -f docker-compose-local.yml up -d`

Then run the API.

### Runbook


### Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)
