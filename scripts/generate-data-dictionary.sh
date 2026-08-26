#!/usr/bin/env bash
#
# Exports the non-associations schema as a flat CSV data dictionary.
#
# The descriptions come from the COMMENT ON statements in db/migration/V1_3__schema_comments.sql, so this
# is the same source of truth as the SchemaSpy report. The sensitivity classification is pulled out of
# the trailing [Sensitivity: ...] tag on each column comment into its own column, and stripped from the
# description so the text reads cleanly. The output is intended for the MOJ Data Catalogue / AWS Glue.
#
# Usage:
#   scripts/generate-data-dictionary.sh [output-file]
#
# Expects a database built by Flyway. Connection details are taken from the environment, defaulting
# to the container in docker-compose-schema-spy.yml:
#   DB_HOST (localhost) DB_PORT (5432) DB_NAME (non_associations) DB_USER (non_associations)
#   DB_PASSWORD (non_associations) DB_SCHEMA (public)

set -euo pipefail

OUTPUT="${1:-data-dictionary.csv}"

DB_HOST="${DB_HOST:-localhost}"
DB_PORT="${DB_PORT:-5432}"
DB_NAME="${DB_NAME:-non_associations}"
DB_USER="${DB_USER:-non_associations}"
DB_PASSWORD="${DB_PASSWORD:-non_associations}"
DB_SCHEMA="${DB_SCHEMA:-public}"

export PGPASSWORD="$DB_PASSWORD"

read -r -d '' QUERY <<SQL || true
SELECT
  c.table_name,
  obj_description(pc.oid)                                AS table_description,
  c.column_name,
  c.ordinal_position,
  c.data_type,
  c.character_maximum_length,
  c.is_nullable,
  c.column_default,
  regexp_replace(
    col_description(pc.oid, c.ordinal_position),
    '\s*\[Sensitivity: [A-Z-]+\]$', ''
  )                                                      AS column_description,
  substring(
    col_description(pc.oid, c.ordinal_position)
    from '\[Sensitivity: ([A-Z-]+)\]'
  )                                                      AS sensitivity,
  CASE WHEN pk.column_name IS NOT NULL THEN 'Y' ELSE 'N' END AS is_primary_key,
  fk.references_table                                    AS foreign_key_references
FROM information_schema.columns c
JOIN pg_class pc
  ON pc.relname = c.table_name
 AND pc.relnamespace = '${DB_SCHEMA}'::regnamespace
 AND pc.relkind = 'r'
LEFT JOIN (
  SELECT kcu.table_name, kcu.column_name
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage kcu
    ON kcu.constraint_name = tc.constraint_name
   AND kcu.table_schema = tc.table_schema
  WHERE tc.constraint_type = 'PRIMARY KEY'
    AND tc.table_schema = '${DB_SCHEMA}'
) pk ON pk.table_name = c.table_name AND pk.column_name = c.column_name
LEFT JOIN (
  SELECT kcu.table_name, kcu.column_name, ccu.table_name AS references_table
  FROM information_schema.table_constraints tc
  JOIN information_schema.key_column_usage kcu
    ON kcu.constraint_name = tc.constraint_name
   AND kcu.table_schema = tc.table_schema
  JOIN information_schema.constraint_column_usage ccu
    ON ccu.constraint_name = tc.constraint_name
   AND ccu.table_schema = tc.table_schema
  WHERE tc.constraint_type = 'FOREIGN KEY'
    AND tc.table_schema = '${DB_SCHEMA}'
) fk ON fk.table_name = c.table_name AND fk.column_name = c.column_name
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

echo "Wrote $(($(wc -l < "$OUTPUT") - 1)) columns to $OUTPUT"
