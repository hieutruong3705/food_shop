-- CATALOG (categories AND brands) is replaced by CATEGORY and BRAND, which never mix, and every
-- scope now reads as a union: whole categories or brands plus single products.
-- An offer that listed both categories and brands selected the products in both, so it becomes an
-- explicit product list and shoppers see no change. Offers with one group keep their group.

INSERT INTO product_promotion(promotion_id, product_id)
 SELECT DISTINCT pc.promotion_id, p.id
 FROM promotion_category pc
 JOIN brand_promotion bp ON bp.promotion_id = pc.promotion_id
 JOIN promotions x ON x.id = pc.promotion_id AND x.scope_type = 'CATALOG'
 JOIN products p ON p.category_id = pc.category_id AND p.brand_id = bp.brand_id;
UPDATE promotions SET scope_type = 'PRODUCT'
 WHERE scope_type = 'CATALOG'
 AND EXISTS (SELECT 1 FROM promotion_category c WHERE c.promotion_id = promotions.id)
 AND EXISTS (SELECT 1 FROM brand_promotion b WHERE b.promotion_id = promotions.id);
DELETE c FROM promotion_category c JOIN promotions p ON p.id = c.promotion_id WHERE p.scope_type = 'PRODUCT';
DELETE b FROM brand_promotion b JOIN promotions p ON p.id = b.promotion_id WHERE p.scope_type = 'PRODUCT';
UPDATE promotions SET scope_type = 'BRAND'
 WHERE scope_type = 'CATALOG' AND EXISTS (SELECT 1 FROM brand_promotion b WHERE b.promotion_id = promotions.id);
UPDATE promotions SET scope_type = 'CATEGORY' WHERE scope_type = 'CATALOG';

INSERT INTO voucher_product(voucher_id, product_id)
 SELECT DISTINCT vc.voucher_id, p.id
 FROM voucher_category vc
 JOIN voucher_brand vb ON vb.voucher_id = vc.voucher_id
 JOIN vouchers x ON x.id = vc.voucher_id AND x.scope_type = 'CATALOG'
 JOIN products p ON p.category_id = vc.category_id AND p.brand_id = vb.brand_id;
UPDATE vouchers SET scope_type = 'PRODUCT'
 WHERE scope_type = 'CATALOG'
 AND EXISTS (SELECT 1 FROM voucher_category c WHERE c.voucher_id = vouchers.id)
 AND EXISTS (SELECT 1 FROM voucher_brand b WHERE b.voucher_id = vouchers.id);
DELETE c FROM voucher_category c JOIN vouchers v ON v.id = c.voucher_id WHERE v.scope_type = 'PRODUCT';
DELETE b FROM voucher_brand b JOIN vouchers v ON v.id = b.voucher_id WHERE v.scope_type = 'PRODUCT';
UPDATE vouchers SET scope_type = 'BRAND'
 WHERE scope_type = 'CATALOG' AND EXISTS (SELECT 1 FROM voucher_brand b WHERE b.voucher_id = vouchers.id);
UPDATE vouchers SET scope_type = 'CATEGORY' WHERE scope_type = 'CATALOG';

-- Lists that do not belong to the scope type are ignored by the app; clear them so the tables agree.
DELETE b FROM brand_promotion b JOIN promotions p ON p.id = b.promotion_id WHERE p.scope_type = 'CATEGORY';
DELETE c FROM promotion_category c JOIN promotions p ON p.id = c.promotion_id WHERE p.scope_type = 'BRAND';
DELETE b FROM voucher_brand b JOIN vouchers v ON v.id = b.voucher_id WHERE v.scope_type = 'CATEGORY';
DELETE c FROM voucher_category c JOIN vouchers v ON v.id = c.voucher_id WHERE v.scope_type = 'BRAND';

ALTER TABLE promotions ALTER COLUMN scope_type SET DEFAULT 'CATEGORY';
ALTER TABLE vouchers ALTER COLUMN scope_type SET DEFAULT 'CATEGORY';
