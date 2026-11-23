-- One row per payment report that was applied. A report that arrives again with the same
-- eventId gets the stored response and changes nothing.
CREATE TABLE payment_report (
    event_id      VARCHAR(100) PRIMARY KEY,
    request_hash  VARCHAR(64)  NOT NULL,
    checkout_id   VARCHAR(100) NOT NULL,
    invoice_id    BIGINT       NOT NULL,
    response_body TEXT         NOT NULL,
    received_at   TIMESTAMPTZ  NOT NULL
);
