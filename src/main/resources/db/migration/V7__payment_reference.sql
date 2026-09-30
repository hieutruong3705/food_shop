-- VNPay transaction number of a verified payment. A payment that arrives after the order
-- was cancelled or expired is flagged REFUND_REQUIRED and keeps this reference for the refund.
ALTER TABLE orders ADD COLUMN payment_reference VARCHAR(64) NULL;
