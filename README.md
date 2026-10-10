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
along with three CSV exports:

| File | Contents |
|------|----------|
| `data-dictionary.csv` | Every table and column, with its description, sensitivity classification, type, nullability, PK and FK, example value and SAR classification. For the MOJ Data Catalogue |
| `sar-data-requirements.csv` | The SAR Data Requirements extract the Offender SAR team review — see [Subject access requests](#subject-access-requests) |
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
scripts/generate-sar-data-requirements.sh
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

Two more tags sit between the description and the sensitivity tag: an example value (`[Example: VIOLENCE]`)
and whether the column's value reaches a prisoner's subject access request report (`[SAR: Y]` or
`[SAR: N]`), both added in `V1_4`. Each tag is split into its own column in `data-dictionary.csv`, and
stripped from the description there so the text reads cleanly.

**Any new table or column needs a `COMMENT ON`** in a migration — `SchemaCommentsTest` fails the build
otherwise. A later migration can add to or replace any comment at any time. Likewise a new enum value
needs a description in `ExportReferenceData`, which fails rather than exporting a blank row.

Note that the compose database binds host port 5432 deliberately: `Testcontainer.isRunning()` defers to
an already-running database, so `InitialiseDatabase` migrates that container and SchemaSpy can read the
same schema afterwards. Left to Testcontainers the schema would die with the JVM.

## Subject access requests

Non-associations is already in the SAR tool, but its report template is still held centrally by the HAA team
and the service has never had a data review under the
[Central SAR Change Control Process](https://dsdmoj.atlassian.net/wiki/spaces/NDSS/pages/6057492803). The route
(epic IR-2036) is to move the template into this repo unchanged, then re-baseline with the Offender SAR team.

`scripts/generate-sar-data-requirements.sh` produces the SAR Data Requirements extract the Offender SAR team
review, and it is published with the schema report. It is generated from the column comments, so it cannot
drift from the schema: every column carries an example value and a SAR classification as well as its
sensitivity, and `SchemaCommentsTest` fails the build if a new column is missing any of them. `[SAR: Y]` means
the value reaches the report under the response proposed for the re-baseline; the Offender SAR team's decision
goes in a later migration, and any new column needs the same decision.

Two ordering rules apply to every change that affects the SAR response or template:

1. No code or template reaches preprod or prod until the Offender SAR team have signed off the test report.
2. The template must be registered with the SAR tool in an environment **before** the code deploys there,
   or the product is suspended. Ask the HAA team on `#haa-sar-functionality-change-request`.

## Architecture

Architecture decision records start [here](doc/architecture/decisions/0001-use-adr.md)
