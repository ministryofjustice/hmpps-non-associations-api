-- Add `updated_by` field to `non_association` table
ALTER TABLE non_association
ADD COLUMN updated_by VARCHAR(30) NOT NULL;
