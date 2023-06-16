-- Type of restriction, e.g. two prisoners can't be in the same cell
CREATE TABLE restriction_type
(
    code         VARCHAR(6)  NOT NULL PRIMARY KEY CHECK (length(code) >= 1),
    description  VARCHAR(30) NOT NULL CHECK (length(description) >= 1),
    when_created TIMESTAMPTZ NOT NULL DEFAULT now(),
    when_updated TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- https://github.com/ministryofjustice/prison-api/blob/acb4076b98c706c49edd61f21061c84eb6a837bf/src/main/resources/db/migration/data/R__2_3__REFERENCE_CODES.sql#LL5185-L5189
INSERT INTO restriction_type (code, description)
VALUES ('CELL', 'Do Not Locate in Same Cell'),
       ('LAND', 'Do Not Locate on Same Landing'),
       ('NONEX', 'Do Not Exercise Together'),
       ('TNA', 'Total Non Association'),
       ('WING', 'Do Not Locate on Same Wing');
