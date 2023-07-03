-- Non-associations table
CREATE TABLE non_association
(
    id                          BIGSERIAL PRIMARY KEY,

    -- prisoners to non-associate and their reasons
    first_prisoner_number       VARCHAR(10) NOT NULL CHECK (length(first_prisoner_number) > 0),
    first_prisoner_reason_code  VARCHAR(6) NOT NULL CHECK (length(first_prisoner_reason_code) > 0),
    second_prisoner_number      VARCHAR(10) NOT NULL CHECK ((length(second_prisoner_number) > 0) AND second_prisoner_number <> first_prisoner_number),
    second_prisoner_reason_code VARCHAR(6) NOT NULL CHECK (length(second_prisoner_reason_code) > 0),

    -- non-association details
    restriction_type_code  VARCHAR(6) NOT NULL CHECK (length(restriction_type_code) > 0),
    comment                TEXT NOT NULL,
    incident_report_number VARCHAR(16),
    authorised_by          VARCHAR(30),

    -- whether the non-association is closed and by who/why/when
    is_closed     BOOLEAN NOT NULL DEFAULT FALSE,
    closed_by     VARCHAR(30) CHECK ((is_closed IS TRUE AND closed_by IS NOT NULL) OR (is_closed IS FALSE AND closed_by IS NULL)),
    closed_reason TEXT CHECK ((is_closed IS TRUE AND closed_reason IS NOT NULL) OR (is_closed IS FALSE AND closed_reason IS NULL)),
    closed_at     TIMESTAMPTZ CHECK ((is_closed IS TRUE AND closed_at IS NOT NULL) OR (is_closed IS FALSE AND closed_at IS NULL)),

    -- record creation/update time
    when_created TIMESTAMPTZ NOT NULL DEFAULT now(),
    when_updated TIMESTAMPTZ NOT NULL DEFAULT now()
);
