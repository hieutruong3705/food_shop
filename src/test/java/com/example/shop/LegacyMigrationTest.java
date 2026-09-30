package com.example.shop;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.flywaydb.core.Flyway;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.core.io.ClassPathResource;
import java.sql.DriverManager;
import static org.assertj.core.api.Assertions.*;

@EnabledIfEnvironmentVariable(named="MIGRATION_TEST_URL",matches="jdbc:mysql://127\\.0\\.0\\.1:330[78]/techshop_migration_[a-z0-9]+.*")
class LegacyMigrationTest {
    @Test void baselinesLegacySchemaWithoutReinterpretingData()throws Exception{
        String url=System.getenv("MIGRATION_TEST_URL"),user=System.getenv("MIGRATION_TEST_USERNAME"),password=System.getenv("MIGRATION_TEST_PASSWORD");
        try(var connection=DriverManager.getConnection(url,user,password)){
            ScriptUtils.executeSqlScript(connection,new ClassPathResource("db/migration/V1__legacy_schema.sql"));
            try(var s=connection.createStatement()){
                s.executeUpdate("INSERT INTO users(id,email,status,is_locked,enabled) VALUES (1,'legacy@example.test',1,0,1)");
                s.executeUpdate("INSERT INTO products(id,name,price,quantity,sold,active) VALUES (1,'Legacy food product',25000,9,1,1)");
                s.executeUpdate("INSERT INTO orders(id,total_price,status,user_id) VALUES (1,25000,'COMPLETED',1)");
                s.executeUpdate("INSERT INTO order_details(id,quantity,price,order_id,product_id) VALUES (1,1,25000,1,1)");
                s.executeUpdate("INSERT INTO vouchers(id,code,discount,is_all) VALUES (1,'LEGACY10',10,1)");
                s.executeUpdate("INSERT INTO categories(id,name) VALUES(1,'Legacy category')");
                s.executeUpdate("INSERT INTO brands(id,name) VALUES(1,'Legacy brand')");
                s.executeUpdate("UPDATE products SET category_id=1,brand_id=1 WHERE id=1");
                s.executeUpdate("INSERT INTO promotions(id,name,discount_rate,active) VALUES(1,'Mixed',7,1),(2,'Brand',5,1)");
                s.executeUpdate("INSERT INTO product_promotion(promotion_id,product_id) VALUES(1,1)");
                s.executeUpdate("INSERT INTO brand_promotion(promotion_id,brand_id) VALUES(1,1),(2,1)");
            }
        }
        var upToV10=Flyway.configure().dataSource(url,user,password).baselineOnMigrate(true).baselineVersion("1").target("10").load();
        assertThat(upToV10.migrate().migrationsExecuted).isEqualTo(9);
        try(var connection=DriverManager.getConnection(url,user,password);var s=connection.createStatement()){
            // Offers saved while the combined CATALOG type existed: both groups, and a category only.
            s.executeUpdate("INSERT INTO promotions(id,name,discount_rate,active,scope_type) VALUES(10,'Both',5,1,'CATALOG'),(11,'CategoryOnly',5,1,'CATALOG')");
            s.executeUpdate("INSERT INTO promotion_category(promotion_id,category_id) VALUES(10,1),(11,1)");
            s.executeUpdate("INSERT INTO brand_promotion(promotion_id,brand_id) VALUES(10,1)");
            s.executeUpdate("INSERT INTO vouchers(id,code,discount,scope_type) VALUES(2,'BOTH',10,'CATALOG')");
            s.executeUpdate("INSERT INTO voucher_category(voucher_id,category_id) VALUES(2,1)");
            s.executeUpdate("INSERT INTO voucher_brand(voucher_id,brand_id) VALUES(2,1)");
        }
        var flyway=Flyway.configure().dataSource(url,user,password).load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(1);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try(var connection=DriverManager.getConnection(url,user,password);var s=connection.createStatement()){
            try(var r=s.executeQuery("SELECT COUNT(*) FROM category_brands WHERE category_id=1 AND brand_id=1")){r.next();assertThat(r.getInt(1)).isEqualTo(1);}
            try(var r=s.executeQuery("SELECT scope_type FROM vouchers WHERE id=1")){r.next();assertThat(r.getString(1)).isEqualTo("ALL");}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM promotions WHERE scope_type='BRAND' AND id IN (1,2)")){r.next();assertThat(r.getInt(1)).isEqualTo(2);}
            try(var r=s.executeQuery("SELECT (SELECT COUNT(*) FROM promotions WHERE scope_type='CATALOG')+(SELECT COUNT(*) FROM vouchers WHERE scope_type='CATALOG')")){r.next();assertThat(r.getInt(1)).isZero();}
            // Categories AND brands became the products in both; one group keeps its group.
            try(var r=s.executeQuery("SELECT p.scope_type,(SELECT COUNT(*) FROM product_promotion x WHERE x.promotion_id=p.id AND x.product_id=1),(SELECT COUNT(*) FROM promotion_category c WHERE c.promotion_id=p.id),(SELECT COUNT(*) FROM brand_promotion b WHERE b.promotion_id=p.id) FROM promotions p WHERE p.id=10")){r.next();assertThat(r.getString(1)).isEqualTo("PRODUCT");assertThat(r.getInt(2)).isEqualTo(1);assertThat(r.getInt(3)).isZero();assertThat(r.getInt(4)).isZero();}
            try(var r=s.executeQuery("SELECT scope_type FROM promotions WHERE id=11")){r.next();assertThat(r.getString(1)).isEqualTo("CATEGORY");}
            try(var r=s.executeQuery("SELECT v.scope_type,(SELECT COUNT(*) FROM voucher_product x WHERE x.voucher_id=v.id AND x.product_id=1),(SELECT COUNT(*) FROM voucher_category c WHERE c.voucher_id=v.id),(SELECT COUNT(*) FROM voucher_brand b WHERE b.voucher_id=v.id) FROM vouchers v WHERE v.id=2")){r.next();assertThat(r.getString(1)).isEqualTo("PRODUCT");assertThat(r.getInt(2)).isEqualTo(1);assertThat(r.getInt(3)).isZero();assertThat(r.getInt(4)).isZero();}
            try(var r=s.executeQuery("SELECT p.discount_rate FROM promotions p JOIN product_promotion x ON x.promotion_id=p.id WHERE p.scope_type='PRODUCT' AND x.product_id=1")){r.next();assertThat(r.getDouble(1)).isEqualTo(7);}
            try(var r=s.executeQuery("SELECT variant_id,variant_label FROM order_details WHERE id=1")){assertThat(r.next()).isTrue();assertThat(r.getObject(1)).isNull();assertThat(r.getString(2)).isNull();}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM product_variants")){assertThat(r.next()).isTrue();assertThat(r.getLong(1)).isZero();}
            try(var r=s.executeQuery("SELECT name,quantity,technology_catalog,sku FROM products WHERE id=1")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("Legacy food product");assertThat(r.getLong(2)).isEqualTo(9);assertThat(r.getBoolean(3)).isFalse();assertThat(r.getString(4)).isEqualTo("LEGACY-1");}
            try(var r=s.executeQuery("SELECT d.product_name,d.price,o.total_price FROM order_details d JOIN orders o ON o.id=d.order_id WHERE d.id=1")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("Legacy food product");assertThat(r.getDouble(2)).isEqualTo(25000);assertThat(r.getDouble(3)).isEqualTo(25000);}
            try(var r=s.executeQuery("SELECT code,active,minimum_order,used_count,usage_limit FROM vouchers WHERE id=1")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("LEGACY10");assertThat(r.getBoolean(2)).isTrue();assertThat(r.getDouble(3)).isZero();assertThat(r.getLong(4)).isZero();assertThat(r.getObject(5)).isNull();}
        }
    }
}
