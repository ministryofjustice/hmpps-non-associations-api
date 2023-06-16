-- Reason why two prisoners should not associate
CREATE TABLE reason_type
(
    code         VARCHAR(6)  NOT NULL PRIMARY KEY CHECK (length(code) >= 1),
    description  VARCHAR(30) NOT NULL CHECK (length(description) >= 1),
    when_created TIMESTAMPTZ NOT NULL DEFAULT now(),
    when_updated TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- https://github.com/ministryofjustice/prison-api/blob/acb4076b98c706c49edd61f21061c84eb6a837bf/src/main/resources/db/migration/data/R__2_3__REFERENCE_CODES.sql#LL5181-L5184
INSERT INTO reason_type (code, description)
VALUES ('BUL', 'Anti Bullying Strategy'),
       ('PER', 'Perpetrator'),
       ('RIV', 'Rival Gang'),
       ('VIC', 'Victim');
