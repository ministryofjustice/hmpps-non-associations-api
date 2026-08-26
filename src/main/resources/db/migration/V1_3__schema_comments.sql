-- Data dictionary for the non-associations schema.
--
-- These comments are read by SchemaSpy (published to GitHub Pages) and by anything else that reads
-- pg_description, including the CSV export for the MOJ Data Catalogue / Glue. Keep them updated when
-- columns are added or their meaning changes - SchemaCommentsTest fails the build if a table or column
-- has no comment.
--
-- Every column comment ends with a sensitivity classification:
--
--   [Sensitivity: NONE]                - not personal data in itself (keys, timestamps, process flags)
--   [Sensitivity: PERSONAL]            - personal data about a prisoner: identifies or locates them
--   [Sensitivity: STAFF]               - personal data about a member of staff, typically the username
--                                        that performed an action
--   [Sensitivity: SPECIAL-CATEGORY]    - UK GDPR Article 9 data (health, sexuality, religion, race,
--                                        gender reassignment) or criminal offence data under Article 10
--   [Sensitivity: OFFICIAL-SENSITIVE]  - not personal data, but damaging if disclosed
--
-- STAFF is still personal data and still in scope for a staff member's own subject access request. It is
-- separated from PERSONAL so that an extract about prisoners can be reasoned about without staff columns
-- inflating the count, and so staff data can be dropped or pseudonymised independently.
--
-- Three things to understand before using these classifications:
--
--   1. They describe the column's own content, not the row's. Every row here concerns two named
--      prisoners, so the whole record is personal data about both of them whatever an individual column
--      is marked - that is what matters for a subject access request. It also means one row belongs to
--      two data subjects, which is unusual in this estate: a SAR for one prisoner covers rows where
--      their number appears in *either* prisoner column, and the other prisoner's number in that row is
--      third-party data.
--   2. **A large share of this small schema is special category data.** A non-association exists because
--      of bullying, violence, gang activity, organised crime or a police request. The reason, the roles
--      and the free text all describe alleged offending in custody, which is criminal offence data under
--      Article 10 (DPA 2018 s.11(2) extends this to alleged offences, not only convictions). The
--      restriction type is the security measure imposed in response, which Article 10 also covers.
--   3. **Every free-text column should be assumed to contain more than its label asks.** comment and
--      closed_reason are written by staff describing an incident in their own words, and in practice
--      name third parties and describe violence, health and offending.
--
-- Note on closed rows: closed_by, closed_reason and closed_at are constrained to be all-null or all-set
-- together with is_closed. A closed non-association is not deleted - the history is retained.

COMMENT ON TABLE non_association IS 'One recorded requirement to keep two prisoners apart, and how far apart. The record concerns both prisoners equally - first and second carry no precedence, and the pair is not stored in a canonical order, so a query about one prisoner must check both columns. Rows are closed rather than deleted when the requirement ends, and can be reopened, so this table is also the history. Non-associations originate either in DPS or from NOMIS, which is why authorised_by is nullable and was free text there.';

COMMENT ON COLUMN non_association.id IS 'Primary key. Surrogate sequence value; carries no meaning and is not the NOMIS identifier. [Sensitivity: NONE]';
COMMENT ON COLUMN non_association.first_prisoner_number IS 'NOMIS offender number (noms id) of one of the two prisoners. First rather than second is an accident of how the record was created and implies nothing - neither prisoner is the subject of the record more than the other. Updated in place when NOMIS merges two offender records. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN non_association.first_prisoner_role IS 'How the first prisoner is involved: VICTIM, PERPETRATOR, NOT_RELEVANT or UNKNOWN. PERPETRATOR records an allegation of offending in custody against a named person, and VICTIM records that they were the subject of it, so this is criminal offence data whichever value it holds. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN non_association.second_prisoner_number IS 'NOMIS offender number (noms id) of the other prisoner. Constrained to differ from first_prisoner_number. In a subject access request for the first prisoner this column is third-party data about someone else. [Sensitivity: PERSONAL]';
COMMENT ON COLUMN non_association.second_prisoner_role IS 'How the second prisoner is involved: VICTIM, PERPETRATOR, NOT_RELEVANT or UNKNOWN. Criminal offence data about that prisoner whichever value it holds - see first_prisoner_role. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN non_association.reason IS 'Why the two prisoners must be kept apart: BULLYING, GANG_RELATED, ORGANISED_CRIME, LEGAL_REQUEST (police or legal request), THREAT, VIOLENCE or OTHER. Every value except OTHER names alleged offending or a police interest in the prisoners, so the code alone is criminal offence data about both of them. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN non_association.restriction_type IS 'How far apart the two must be kept: CELL (cell only), LANDING (cell and landing) or WING (cell, landing and wing). This is the security measure imposed in response to alleged offending, which Article 10 covers alongside the offence data itself, and its severity is a proxy for how serious the risk was judged to be. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN non_association.comment IS 'Free-text explanation of why the two prisoners must be kept apart, written by the member of staff recording or updating it. Unstructured and unbounded - in practice describes violence, threats, gang affiliation, health and third parties, so treat as special category regardless of what any individual comment happens to say. Overwritten with the reopening reason when a closed non-association is reopened, so it holds the current explanation rather than the original one. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN non_association.when_created IS 'When the non-association was first recorded in this service. For records migrated from NOMIS this is the migration, not the original prison event. [Sensitivity: NONE]';
COMMENT ON COLUMN non_association.when_updated IS 'When the non-association was last changed, including being closed or reopened. Equal to when_created until the first change. [Sensitivity: NONE]';
COMMENT ON COLUMN non_association.authorised_by IS 'Who created or authorised the non-association. Nullable and not reliably a username: it was a free-text field in NOMIS and was not set at all by this service''s early write paths, so it holds hand-typed names as well as DPS usernames. Identifies a member of staff either way. [Sensitivity: STAFF]';
COMMENT ON COLUMN non_association.updated_by IS 'DPS username of the member of staff who last changed the record, or the system user for changes made by the NOMIS merge listener. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN non_association.is_closed IS 'Whether the requirement to keep the two prisoners apart has ended. Closed rows are retained rather than deleted, and can be reopened, so this is a state flag rather than a soft delete. The three closed_* columns are constrained to be all-set when true and all-null when false. [Sensitivity: NONE]';
COMMENT ON COLUMN non_association.closed_by IS 'DPS username of the member of staff who closed the non-association. Null while it is open. Identifies a member of staff. [Sensitivity: STAFF]';
COMMENT ON COLUMN non_association.closed_reason IS 'Free-text explanation of why the non-association was closed, for example that one prisoner has been released or transferred, or that the risk was reassessed. Null while it is open. Unstructured and written by hand, so assume it describes offending, health and third parties like comment does. [Sensitivity: SPECIAL-CATEGORY]';
COMMENT ON COLUMN non_association.closed_at IS 'When the non-association was closed. Null while it is open. [Sensitivity: NONE]';
