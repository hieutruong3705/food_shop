-- Additive migration. Existing records, orders and relationships are retained.
ALTER TABLE products ADD COLUMN sku VARCHAR(255), ADD COLUMN slug VARCHAR(255), ADD COLUMN warranty_months INT,
 ADD COLUMN technology_catalog BIT NOT NULL DEFAULT 0, ADD CONSTRAINT uk_products_sku UNIQUE(sku), ADD CONSTRAINT uk_products_slug UNIQUE(slug);
UPDATE products SET sku=CONCAT('LEGACY-',id), slug=CONCAT('legacy-',id) WHERE sku IS NULL;
ALTER TABLE banners ADD COLUMN technology_catalog BIT NOT NULL DEFAULT 0;
ALTER TABLE policies ADD COLUMN technology_catalog BIT NOT NULL DEFAULT 0;
ALTER TABLE orders ADD COLUMN checkout_key VARCHAR(100), ADD COLUMN checkout_fingerprint VARCHAR(255),
 ADD COLUMN shipping_fee DOUBLE, ADD COLUMN discount_amount DOUBLE, ADD COLUMN voucher_code VARCHAR(255),
 ADD CONSTRAINT uk_orders_checkout_key UNIQUE(checkout_key), MODIFY receiver_address VARCHAR(500);
ALTER TABLE order_details ADD COLUMN product_name VARCHAR(255), ADD COLUMN product_sku VARCHAR(255);
-- Historical names cannot be reconstructed if they were changed before migration.
UPDATE order_details d JOIN products p ON d.product_id=p.id SET d.product_name=p.name,d.product_sku=p.sku WHERE d.product_name IS NULL;
ALTER TABLE users ADD COLUMN reset_password_expires_at DATETIME(6), ADD COLUMN verification_expires_at DATETIME(6), MODIFY address VARCHAR(500);
ALTER TABLE product_specs MODIFY spec_value VARCHAR(500);
ALTER TABLE promotions MODIFY description TEXT;
CREATE INDEX ix_orders_user_created ON orders(user_id,created_at);
CREATE INDEX ix_products_catalog ON products(technology_catalog,active,category_id,brand_id);
