CREATE TABLE roles (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), description VARCHAR(255));
CREATE TABLE users (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, email VARCHAR(255) NOT NULL, password VARCHAR(255), full_name VARCHAR(255),
 phone VARCHAR(255), address VARCHAR(255), status BIGINT NOT NULL, is_locked BIT NOT NULL, enabled BIT NOT NULL,
 reset_password_token VARCHAR(255), verification_code VARCHAR(64), role_id BIGINT,
 CONSTRAINT uk_users_email UNIQUE(email), FOREIGN KEY(role_id) REFERENCES roles(id)
);
CREATE TABLE categories (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, description TEXT, image VARCHAR(255));
CREATE TABLE brands (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, description TEXT, logo_url VARCHAR(255), UNIQUE(name));
CREATE TABLE products (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255) NOT NULL, price DOUBLE NOT NULL, active BOOLEAN DEFAULT TRUE,
 detail_desc TEXT, short_desc TEXT, quantity BIGINT NOT NULL, sold BIGINT NOT NULL, factory VARCHAR(255), target VARCHAR(255), origin VARCHAR(255), category_id BIGINT, brand_id BIGINT,
 FOREIGN KEY(category_id) REFERENCES categories(id), FOREIGN KEY(brand_id) REFERENCES brands(id)
);
CREATE TABLE product_images (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, image_url VARCHAR(255), product_id BIGINT, FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE product_specs (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, spec_name VARCHAR(255), spec_value VARCHAR(255), product_id BIGINT, FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE carts (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, sum INT NOT NULL, user_id BIGINT, UNIQUE(user_id), FOREIGN KEY(user_id) REFERENCES users(id));
CREATE TABLE cart_items (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, quantity BIGINT NOT NULL, price DOUBLE NOT NULL, cart_id BIGINT, product_id BIGINT, FOREIGN KEY(cart_id) REFERENCES carts(id), FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE orders (
 id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, total_price DOUBLE NOT NULL, receiver_name VARCHAR(255), receiver_address VARCHAR(255), receiver_phone VARCHAR(255),
 status VARCHAR(255), payment_method VARCHAR(255), payment_status VARCHAR(255), created_at DATETIME(6), completed_at DATETIME(6), user_id BIGINT, FOREIGN KEY(user_id) REFERENCES users(id)
);
CREATE TABLE order_details (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, quantity BIGINT NOT NULL, price DOUBLE NOT NULL, order_id BIGINT, product_id BIGINT, FOREIGN KEY(order_id) REFERENCES orders(id), FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE promotions (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), description VARCHAR(255), discount_rate DOUBLE NOT NULL, start_date DATETIME(6), end_date DATETIME(6), active BIT NOT NULL);
CREATE TABLE product_promotion (promotion_id BIGINT NOT NULL, product_id BIGINT NOT NULL, FOREIGN KEY(promotion_id) REFERENCES promotions(id), FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE brand_promotion (promotion_id BIGINT NOT NULL, brand_id BIGINT NOT NULL, FOREIGN KEY(promotion_id) REFERENCES promotions(id), FOREIGN KEY(brand_id) REFERENCES brands(id));
CREATE TABLE vouchers (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, code VARCHAR(255), discount DOUBLE NOT NULL, is_all BIT NOT NULL);
CREATE TABLE voucher_category (voucher_id BIGINT NOT NULL, category_id BIGINT NOT NULL, FOREIGN KEY(voucher_id) REFERENCES vouchers(id), FOREIGN KEY(category_id) REFERENCES categories(id));
CREATE TABLE reviews (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, rating INT NOT NULL, content TEXT, created_at DATETIME(6), user_id BIGINT, product_id BIGINT, FOREIGN KEY(user_id) REFERENCES users(id), FOREIGN KEY(product_id) REFERENCES products(id));
CREATE TABLE banners (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), image_url VARCHAR(255), link VARCHAR(255), active BOOLEAN DEFAULT TRUE);
CREATE TABLE policies (id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY, name VARCHAR(255), content TEXT, active BIT NOT NULL);
