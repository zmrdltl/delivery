-- Existing databases: run once before starting the version with Payment.Status.
-- Fresh databases: Hibernate ddl-auto=update creates this column from the entity.
-- Keep previous payment records and initialize their status to PAID.
BEGIN;

ALTER TABLE payments
    ADD COLUMN status VARCHAR(255) NOT NULL DEFAULT 'PAID';

ALTER TABLE payments
    ADD CONSTRAINT payments_status_check
    CHECK (status IN ('PAID', 'CANCELED'));

COMMIT;
