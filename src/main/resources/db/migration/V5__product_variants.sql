CREATE TABLE product_variants (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
 product_id BIGINT NOT NULL, sku VARCHAR(80) NOT NULL UNIQUE,
 price DOUBLE NOT NULL, quantity BIGINT NOT NULL, active BIT NOT NULL,
 image_url VARCHAR(500), FOREIGN KEY(product_id) REFERENCES products(id)
);
CREATE TABLE variant_options (
 variant_id BIGINT NOT NULL, option_name VARCHAR(80) NOT NULL, option_value VARCHAR(100),
 PRIMARY KEY(variant_id,option_name), FOREIGN KEY(variant_id) REFERENCES product_variants(id)
);
ALTER TABLE cart_items ADD COLUMN variant_id BIGINT, ADD FOREIGN KEY(variant_id) REFERENCES product_variants(id);
ALTER TABLE order_details ADD COLUMN variant_id BIGINT, ADD COLUMN variant_label VARCHAR(2000), ADD FOREIGN KEY(variant_id) REFERENCES product_variants(id);
ALTER TABLE product_images ADD COLUMN sort_order INT NOT NULL DEFAULT 0;
