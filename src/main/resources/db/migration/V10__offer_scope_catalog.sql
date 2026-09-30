-- Keep the meaning of old single-group scopes. Existing join rows are unchanged.
UPDATE promotions SET scope_type = 'CATALOG' WHERE scope_type IN ('CATEGORY', 'BRAND');
UPDATE vouchers SET scope_type = 'CATALOG' WHERE scope_type IN ('CATEGORY', 'BRAND');
ALTER TABLE promotions ALTER COLUMN scope_type SET DEFAULT 'CATALOG';
ALTER TABLE vouchers ALTER COLUMN scope_type SET DEFAULT 'CATALOG';
ALTER TABLE vouchers DROP COLUMN is_all;
