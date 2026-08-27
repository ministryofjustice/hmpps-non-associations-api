# HMPPS Non-associations API

[![Docker Repository on ghcr](https://img.shields.io/badge/ghcr.io-repository-2496ED.svg?logo=docker)](https://ghcr.io/ministryofjustice/hmpps-non-associations-api)
[![Runbook](https://img.shields.io/badge/runbook-view-172B4D.svg?logo=confluence)](https://dsdmoj.atlassian.net/wiki/spaces/NOM/pages/1739325587/DPS+Runbook)
[![API docs](https://img.shields.io/badge/API_docs_-view-85EA2D.svg?logo=swagger)](https://non-associations-api-dev.hmpps.service.justice.gov.uk/swagger-ui/index.html)
[![Event docs](https://img.shields.io/badge/Event_docs-view-85EA2D.svg)](https://studio.asyncapi.com/?url=https://raw.githubusercontent.com/ministryofjustice/hmpps-non-associations-api/main/async-api.yml&readOnly)

This application is the REST api and database that owns prisoner non-association data.

## Running locally against dev/T3 services

This is straight-forward as authentication is delegated down to the calling services in `dev` environment.

Use all environment variables starting with `API_BASE_URL_` from [helm chart values](./helm_deploy/values-dev.yaml).
Choose a suitable hmpps-auth oauth client, for instance from kubernetes `hmpps-incentives-api` secret and add
`NON_ASSOCIATIONS_API_CLIENT_ID` and `NON_ASSOCIATIONS_API_CLIENT_SECRET`.

Start the database and other required services via docker-compose with:

```shell
docker compose -f docker-compose-local.yml up
```

Then run the API; for example using IntelliJ.

## Testing and linting

Run unit and integration tests with:

```shell
./gradlew test
```

Run automatic lint fixes:

```shell
./gradlew ktlintformat
```

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

## Database schema

A browsable schema report is published from `main` to
[ministryofjustice.github.io/hmpps-non-associations-api/schema-spy-report](https://ministryofjustice.github.io/hmpps-non-associations-api/schema-spy-report/),
along with two CSV exports for the MOJ Data Catalogue:

| File | Contents |
|------|----------|
| `data-dictionary.csv` | Every table and column, with its description, sensitivity classification, type, nullability, PK and FK |
| `reference-data.csv` | The enum lookups. Every code in this schema resolves in Kotlin — there are no reference tables — so without this a consumer sees a `varchar(20)` with no idea which values are legal |

The report shows every table and column, with types, nullability, primary and foreign keys, and ER
diagrams. Share it rather than a hand-written description when explaining the schema — to the Data Hub
transition team, or when working out what a subject access request covers.

It is generated from a database built by Flyway, so it cannot drift from the migrations. To regenerate
it locally:

```shell
docker compose -f docker-compose-schema-spy.yml up -d --wait
./gradlew -Pinit-db=true test --tests '*InitialiseDatabase' --tests '*ExportReferenceData'
docker run --rm --network host -v /tmp/schemaspy:/output schemaspy/schemaspy:6.2.4 \
  -t pgsql -host localhost -port 5432 -db non_associations -s public \
  -u non_associations -p non_associations -vizjs
scripts/generate-data-dictionary.sh
```

### Table and column descriptions

Descriptions live in the database as `COMMENT ON` statements, applied by
`db/migration/V1_3__schema_comments.sql`, so SchemaSpy and any Glue crawl read the same source of
truth. Each column description ends with a sensitivity classification:

| Tag | Meaning |
| --- | --- |
| `[Sensitivity: NONE]` | Not personal data in itself |
| `[Sensitivity: PERSONAL]` | Personal data about a prisoner — identifies or locates them |
| `[Sensitivity: STAFF]` | Personal data about a member of staff, typically the username that acted |
| `[Sensitivity: SPECIAL-CATEGORY]` | UK GDPR Article 9 data, or offence data under Article 10 |
| `[Sensitivity: OFFICIAL-SENSITIVE]` | Not personal data, but damaging if disclosed |

`STAFF` is still personal data and still in scope for a staff member's own subject access request. It
is separated from `PERSONAL` so an extract about prisoners can be reasoned about without staff columns
inflating the count.

Two caveats when reading the tags. They describe **the column's own content, not the row's** — every
row here concerns two named prisoners, so the whole record is personal data about both whatever an
individual column is marked. And note that one row has **two** data subjects: a subject access request
for one prisoner covers rows where their number appears in *either* prisoner column, and the other
prisoner's number in that row is third-party data.

Most of this schema is special category data. A non-association exists because of bullying, violence,
gang activity, organised crime or a police request, so the reason, the roles and the free text all
describe alleged offending in custody — criminal offence data under Article 10.

The tag is split into its own `sensitivity` column in `data-dictionary.csv`, and stripped from the
description there so the text reads cleanly.

**Any new table or column needs a `COMMENT ON`** in a migration — `SchemaCommentsTest` fails the build
otherwise. A later migration can add to or replace any comment at any time. Likewise a new enum value
needs a description in `ExportReferenceData`, which fails rather than exporting a blank row.

Note that the compose database binds host port 5432 deliberately: `Testcontainer.isRunning()` defers to
an already-running database, so `InitialiseDatabase` migrates that container and SchemaSpy can read the
same schema afterwards. Left to Testcontainers the schema would die with the JVM.

## Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)
