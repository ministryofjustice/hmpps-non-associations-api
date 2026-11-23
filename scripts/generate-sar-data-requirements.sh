#!/usr/bin/env bash
#
# Exports the SAR Data Requirements extract that the Offender SAR team review.
#
# This is the document behind the data review checkpoint of the Central SAR Change Control Process
# (IR-2042, epic IR-2036). It is generated rather than hand-written for the same reason the data dictionary
# is: the schema comments are the one source of truth, so the extract cannot drift from the schema.
#
# Columns are those the Offender SAR team ask for:
#
#   Product Name, Type of Change, Entity, Element, Description, Example Value, Mandatory, SAR Impact, Impact
#
# Example Value and SAR Impact come from the [Example: ...] and [SAR: Y|N] tags on each column comment, set
# per column in V1_4__sar_data_requirements.sql. SAR Impact answers the question the Offender SAR team are
# actually asking: does this element's value reach the prisoner's report. It is deliberately not derived from
# the [Sensitivity: ...] tag, which answers a different question - whether the column's own content is
# personal data.
#
# Type of Change is RE-BASELINING: non-associations is already in the SAR tool but has never had a data review.
# Impact defaults to NO CHANGE, but a re-baseline changes what the report shows, so on the copy sent to the
# Offender SAR team Impact is set per element against the current report, and the fields the current report
# shows that are not database columns (names, prison and cell from prisoner-search, the open and closed
# counts) are added by hand.
#
# Usage:
#   scripts/generate-sar-data-requirements.sh [output-file]
#
# Expects a database built by Flyway. Connection details are taken from the environment, defaulting
# to the container in docker-compose-schema-spy.yml:
#   DB_HOST (localhost) DB_PORT (5432) DB_NAME (non_associations) DB_USER (non_associations)
#   DB_PASSWORD (non_associations) DB_SCHEMA (public)
#
# PRODUCT_NAME, CHANGE_TYPE and IMPACT can be overridden the same way.

set -euo pipefail

OUTPUT="${1:-sar-data-requirements.csv}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-non_associations}"
DB_USER="${DB_USER:-non_associations}"
DB_PASSWORD="${DB_PASSWORD:-non_associations}"
DB_SCHEMA="${DB_SCHEMA:-public}"

PRODUCT_NAME="${PRODUCT_NAME:-Non-associations}"
CHANGE_TYPE="${CHANGE_TYPE:-RE-BASELINING}"
IMPACT="${IMPACT:-NO CHANGE}"

export PGPASSWORD="$DB_PASSWORD"

read -r -d '' QUERY <<SQL || true
SELECT
  '${PRODUCT_NAME}'                                      AS "Product Name",
  '${CHANGE_TYPE}'                                       AS "Type of Change",
  c.table_name                                           AS "Entity",
  c.column_name                                          AS "Element",
  regexp_replace(
    col_description(pc.oid, c.ordinal_position),
    '\s*\[(Example|SAR|Sensitivity): [^\]]*\]', '', 'g'
  )                                                      AS "Description",
  substring(
    col_description(pc.oid, c.ordinal_position)
    from '\[Example: ([^\]]*)\]'
  )                                                      AS "Example Value",
  CASE WHEN c.is_nullable = 'NO' THEN 'Y' ELSE 'N' END   AS "Mandatory",
  substring(
    col_description(pc.oid, c.ordinal_position)
    from '\[SAR: ([YN])\]'
  )                                                      AS "SAR Impact",
  '${IMPACT}'                                            AS "Impact"
FROM information_schema.columns c
JOIN pg_class pc
  ON pc.relname = c.table_name
 AND pc.relnamespace = '${DB_SCHEMA}'::regnamespace
 AND pc.relkind = 'r'
WHERE c.table_schema = '${DB_SCHEMA}'
  AND c.table_name <> 'flyway_schema_history'
ORDER BY c.table_name, c.ordinal_position
SQL

COPY_COMMAND="COPY ($QUERY) TO STDOUT WITH (FORMAT csv, HEADER true)"

if command -v psql > /dev/null 2>&1; then
  psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -c "$COPY_COMMAND" > "$OUTPUT"
else
  # No local client - use the postgres image. host.docker.internal resolves on Docker Desktop,
  # and --add-host makes it resolve on Linux too.
  docker run --rm --add-host=host.docker.internal:host-gateway \
    -e PGPASSWORD="$DB_PASSWORD" postgres:16 \
    psql -h "$([ "$DB_HOST" = "localhost" ] && echo host.docker.internal || echo "$DB_HOST")" \
    -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
    -v ON_ERROR_STOP=1 -c "$COPY_COMMAND" > "$OUTPUT"
fi

echo "Wrote $(($(wc -l < "$OUTPUT") - 1)) elements to $OUTPUT"
