-- Preserve existing percentage vouchers and order history.
ALTER TABLE vouchers
 ADD COLUMN active BIT NOT NULL DEFAULT 1,
 ADD COLUMN start_date DATETIME(6),
 ADD COLUMN end_date DATETIME(6),
 ADD COLUMN minimum_order DOUBLE NOT NULL DEFAULT 0,
 ADD COLUMN maximum_discount DOUBLE,
 ADD COLUMN usage_limit BIGINT,
 ADD COLUMN used_count BIGINT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN note VARCHAR(1000);
