-- Non-associations table
CREATE TABLE non_association
(
    id                          BIGSERIAL PRIMARY KEY,

    -- prisoners to non-associate and their reasons
    first_prisoner_number       VARCHAR(10) NOT NULL,
    first_prisoner_reason_code  VARCHAR(6) NOT NULL,
    second_prisoner_number      VARCHAR(10) NOT NULL,
    second_prisoner_reason_code VARCHAR(6) NOT NULL,

    -- non-association details
    restriction_type_code  VARCHAR(6) NOT NULL,
    comment                TEXT NOT NULL,
    incident_report_number VARCHAR(16),
    authorised_by          VARCHAR(30),

    -- whether the non-association is closed and by who/why/when
    is_closed     BOOLEAN NOT NULL DEFAULT FALSE,
    closed_by     VARCHAR(30),
    closed_reason TEXT,
    closed_at     TIMESTAMPTZ,

    -- record creation/update time
    when_created TIMESTAMPTZ NOT NULL DEFAULT now(),
    when_updated TIMESTAMPTZ NOT NULL DEFAULT now()
);
