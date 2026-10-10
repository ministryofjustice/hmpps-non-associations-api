-- Example values and subject access request (SAR) classification for every column (IR-2040).
--
-- These feed the SAR Data Requirements extract (scripts/generate-sar-data-requirements.sh), the document the
-- Offender SAR team review at the data review checkpoint of the SAR change control process. Non-associations
-- is already in the SAR tool, but has never had a data review, so it is being re-baselined (IR-2042, epic
-- IR-2036). Every element needs an example value and a statement of whether it reaches a prisoner's report.
-- Comments only - no schema or data change.
--
-- Each column comment gains two tags, between the description and the existing sensitivity tag:
--
--   '... description text. [Example: VIOLENCE] [SAR: Y] [Sensitivity: SPECIAL-CATEGORY]'
--
-- [SAR: Y] means the value reaches the report under the response proposed for the re-baseline (IR-2043).
-- [SAR: N] is for the internal key. That is a proposal for the Offender SAR team to confirm or change; their
-- decision goes in a later migration rather than an edit to this one.
--
-- Every row names two prisoners, so a prisoner's report includes the other prisoner's number and role - data
-- about a third party. Redacting third-party data is the Offender SAR team's job, so the proposal is to return
-- it and let them redact. That default is one of the questions put to them at the data review. The free-text
-- columns (comment, closed_reason) routinely describe other people too, and are proposed on the same basis.
--
-- Example values for code columns are real codes. Every other example is invented, and the free-text examples
-- are deliberately bland: they illustrate the kind of content, not real cases.

DO $$
DECLARE
  rec      record;
  existing text;
BEGIN
  FOR rec IN
    SELECT * FROM (VALUES
      ('non_association', 'id', '12345', 'N'),
      ('non_association', 'first_prisoner_number', 'A1234BC', 'Y'),
      ('non_association', 'first_prisoner_role', 'VICTIM', 'Y'),
      ('non_association', 'second_prisoner_number', 'D5678EF', 'Y'),
      ('non_association', 'second_prisoner_role', 'PERPETRATOR', 'Y'),
      ('non_association', 'reason', 'VIOLENCE', 'Y'),
      ('non_association', 'restriction_type', 'LANDING', 'Y'),
      ('non_association', 'comment', 'Keep apart following an argument on the wing.', 'Y'),
      ('non_association', 'when_created', '2026-03-14T09:30:00', 'Y'),
      ('non_association', 'when_updated', '2026-04-02T15:10:00', 'Y'),
      ('non_association', 'authorised_by', 'AUSER_GEN', 'Y'),
      ('non_association', 'updated_by', 'AUSER_GEN', 'Y'),
      ('non_association', 'is_closed', 'false', 'Y'),
      ('non_association', 'closed_by', 'BUSER_GEN', 'Y'),
      ('non_association', 'closed_reason', 'One prisoner has been transferred.', 'Y'),
      ('non_association', 'closed_at', '2026-05-20T11:00:00', 'Y')
    ) AS t(table_name, column_name, example, sar_impact)
  LOOP
    SELECT col_description(a.attrelid, a.attnum)
      INTO existing
      FROM pg_attribute a
     WHERE a.attrelid = format('%I', rec.table_name)::regclass
       AND a.attname = rec.column_name
       AND a.attnum > 0
       AND NOT a.attisdropped;

    IF existing IS NULL THEN
      RAISE EXCEPTION 'No comment on %.% - describe the column before classifying it',
        rec.table_name, rec.column_name;
    END IF;

    IF position(' [Sensitivity:' IN existing) = 0 THEN
      RAISE EXCEPTION 'Comment on %.% has no sensitivity tag to place the new tags before',
        rec.table_name, rec.column_name;
    END IF;

    IF position('[Example:' IN existing) > 0 OR position('[SAR:' IN existing) > 0 THEN
      RAISE EXCEPTION 'Comment on %.% is already tagged', rec.table_name, rec.column_name;
    END IF;

    IF position(']' IN rec.example) > 0 THEN
      RAISE EXCEPTION 'Example for %.% contains "]", which would end the tag early',
        rec.table_name, rec.column_name;
    END IF;

    EXECUTE format(
      'COMMENT ON COLUMN %I.%I IS %L',
      rec.table_name,
      rec.column_name,
      replace(
        existing,
        ' [Sensitivity:',
        ' [Example: ' || rec.example || '] [SAR: ' || rec.sar_impact || '] [Sensitivity:'
      )
    );
  END LOOP;
END $$;
