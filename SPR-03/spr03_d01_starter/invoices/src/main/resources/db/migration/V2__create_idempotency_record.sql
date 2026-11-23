-- One row per invoice create that carried an Idempotency-Key.
-- The stored response is returned when the same key arrives again with the same content.
CREATE TABLE idempotency_record (
    idempotency_key VARCHAR(255) PRIMARY KEY,
    request_hash    VARCHAR(64)  NOT NULL,
    response_status INT          NOT NULL,
    response_body   TEXT         NOT NULL,
    location        VARCHAR(500) NOT NULL,
    created_at      TIMESTAMPTZ  NOT NULL
);
