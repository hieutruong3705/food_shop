-- Archive removes completed/cancelled orders from the admin work queue only.
-- Customer history, line snapshots and revenue are retained.
ALTER TABLE orders ADD COLUMN admin_archived BIT NOT NULL DEFAULT 0;
