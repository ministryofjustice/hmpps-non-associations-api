-- Increase length of `authorised_by` column to 60 chars as in NOMIS
ALTER TABLE non_association
ALTER COLUMN authorised_by TYPE VARCHAR(60);
