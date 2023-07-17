-- Increase length of `authorised_by` column to 60 chars as in NOMIS
ALTER TABLE non_association
ADD CONSTRAINT prisoner_association_unq UNIQUE (first_prisoner_number, second_prisoner_number)
