CREATE TABLE category_brands (
 category_id BIGINT NOT NULL, brand_id BIGINT NOT NULL,
 PRIMARY KEY(category_id,brand_id), FOREIGN KEY(category_id) REFERENCES categories(id), FOREIGN KEY(brand_id) REFERENCES brands(id)
);
INSERT INTO category_brands(category_id,brand_id)
 SELECT DISTINCT category_id,brand_id FROM products WHERE category_id IS NOT NULL AND brand_id IS NOT NULL;

ALTER TABLE promotions ADD COLUMN scope_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCT';
ALTER TABLE vouchers ADD COLUMN scope_type VARCHAR(20) NOT NULL DEFAULT 'CATEGORY';
CREATE TABLE promotion_category (promotion_id BIGINT NOT NULL,category_id BIGINT NOT NULL,PRIMARY KEY(promotion_id,category_id),FOREIGN KEY(promotion_id) REFERENCES promotions(id),FOREIGN KEY(category_id) REFERENCES categories(id));
CREATE TABLE voucher_brand (voucher_id BIGINT NOT NULL,brand_id BIGINT NOT NULL,PRIMARY KEY(voucher_id,brand_id),FOREIGN KEY(voucher_id) REFERENCES vouchers(id),FOREIGN KEY(brand_id) REFERENCES brands(id));
CREATE TABLE voucher_product (voucher_id BIGINT NOT NULL,product_id BIGINT NOT NULL,PRIMARY KEY(voucher_id,product_id),FOREIGN KEY(voucher_id) REFERENCES vouchers(id),FOREIGN KEY(product_id) REFERENCES products(id));
UPDATE vouchers SET scope_type=CASE WHEN is_all=1 THEN 'ALL' ELSE 'CATEGORY' END;
DELETE vc FROM voucher_category vc JOIN vouchers v ON v.id=vc.voucher_id WHERE v.scope_type='ALL';

-- A legacy promotion may target both brands and products. Split that OR into two
-- equivalent offers, retaining brand eligibility for future products too.
CREATE TEMPORARY TABLE legacy_mixed_promotions AS
 SELECT p.id old_id,(SELECT COALESCE(MAX(id),0) FROM promotions)+ROW_NUMBER() OVER(ORDER BY p.id) new_id
 FROM promotions p WHERE EXISTS(SELECT 1 FROM brand_promotion b WHERE b.promotion_id=p.id)
 AND EXISTS(SELECT 1 FROM product_promotion x WHERE x.promotion_id=p.id);
INSERT INTO promotions(id,name,description,discount_rate,start_date,end_date,active,scope_type)
 SELECT m.new_id,CONCAT(LEFT(p.name,80),' (sản phẩm)'),p.description,p.discount_rate,p.start_date,p.end_date,p.active,'PRODUCT'
 FROM promotions p JOIN legacy_mixed_promotions m ON m.old_id=p.id;
INSERT INTO product_promotion(promotion_id,product_id)
 SELECT m.new_id,x.product_id FROM product_promotion x JOIN legacy_mixed_promotions m ON m.old_id=x.promotion_id;
DELETE x FROM product_promotion x JOIN legacy_mixed_promotions m ON m.old_id=x.promotion_id;
DROP TEMPORARY TABLE legacy_mixed_promotions;
UPDATE promotions p SET scope_type='BRAND' WHERE EXISTS(SELECT 1 FROM brand_promotion b WHERE b.promotion_id=p.id);
