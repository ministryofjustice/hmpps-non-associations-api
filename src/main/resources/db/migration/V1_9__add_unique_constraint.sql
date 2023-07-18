-- Add unique constraint to two prisoner numbers
ALTER TABLE non_association
    ADD CONSTRAINT prisoner_association_unq UNIQUE (first_prisoner_number, second_prisoner_number);

CREATE UNIQUE INDEX prisoner_association_unq_idx ON non_association (first_prisoner_number, second_prisoner_number);