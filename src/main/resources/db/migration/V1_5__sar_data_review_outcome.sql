-- Outcome of the Offender SAR team's review of non-associations (IR-2042, IR-2043).
--
-- On 12 November 2026 the Offender SAR team's senior management decided that non-associations information must
-- not be disclosed in a subject access request (SAR) report. The report instead lists each non-association in a
-- fully sanitised form, with every detail shown as XXXXXX. No column's value reaches the report any more, so
-- every [SAR: Y] proposed in V1_4__sar_data_requirements.sql becomes [SAR: N].
-- Comments only - no schema or data change.

DO $$
DECLARE
  rec      record;
  existing text;
BEGIN
  FOR rec IN
    SELECT a.attrelid::regclass::text AS table_name, a.attname AS column_name, col_description(a.attrelid, a.attnum) AS comment
      FROM pg_attribute a
     WHERE a.attrelid = 'non_association'::regclass
       AND a.attnum > 0
       AND NOT a.attisdropped
  LOOP
    existing := rec.comment;

    IF existing IS NULL OR position('[SAR: ' IN existing) = 0 THEN
      RAISE EXCEPTION 'Comment on %.% has no SAR tag', rec.table_name, rec.column_name;
    END IF;

    IF position('[SAR: Y]' IN existing) > 0 THEN
      EXECUTE format(
        'COMMENT ON COLUMN %I.%I IS %L',
        rec.table_name,
        rec.column_name,
        replace(existing, '[SAR: Y]', '[SAR: N]')
      );
    END IF;
  END LOOP;
END $$;
