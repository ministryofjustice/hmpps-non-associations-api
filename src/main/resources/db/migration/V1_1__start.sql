CREATE TABLE non_association
(
    id                     BIGSERIAL PRIMARY KEY,

    first_prisoner_number  VARCHAR(10) NOT NULL CHECK (length(first_prisoner_number) > 0),
    first_prisoner_role    VARCHAR(20) NOT NULL CHECK (length(first_prisoner_role) > 0),
    second_prisoner_number VARCHAR(10) NOT NULL CHECK (length(second_prisoner_number) > 0),
    second_prisoner_role   VARCHAR(20) NOT NULL CHECK (length(second_prisoner_role) > 0),

    reason                 VARCHAR(20) NOT NULL CHECK (length(reason) > 0),
    restriction_type       VARCHAR(20) NOT NULL CHECK (length(restriction_type) > 0),
    comment                TEXT        NOT NULL CHECK (length(comment) > 0),

    when_created           TIMESTAMPTZ NOT NULL DEFAULT now(),
    when_updated           TIMESTAMPTZ NOT NULL DEFAULT now(),
    authorised_by          VARCHAR(30),

    updated_by             VARCHAR(30) NOT NULL,

    is_closed              BOOL        NOT NULL DEFAULT false,
    closed_by              VARCHAR(30) CHECK (is_closed IS TRUE AND closed_by IS NOT NULL OR is_closed IS FALSE AND closed_by IS NULL),
    closed_reason          TEXT CHECK (is_closed IS TRUE AND closed_reason IS NOT NULL OR is_closed IS FALSE AND closed_reason IS NULL),
    closed_at              TIMESTAMPTZ CHECK (is_closed IS TRUE AND closed_at IS NOT NULL OR is_closed IS FALSE AND closed_at IS NULL),

    CONSTRAINT non_association_different_people CHECK (NOT second_prisoner_number = first_prisoner_number)
);

CREATE INDEX non_association_idx_p1 ON non_association (first_prisoner_number);
CREATE INDEX non_association_idx_p2 ON non_association (second_prisoner_number);
CREATE INDEX non_association_idx_p1p2 ON non_association (first_prisoner_number, second_prisoner_number);
