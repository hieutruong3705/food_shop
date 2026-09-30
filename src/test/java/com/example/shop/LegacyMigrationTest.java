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
        var flyway=Flyway.configure().dataSource(url,user,password).baselineOnMigrate(true).baselineVersion("1").load();
        assertThat(flyway.migrate().migrationsExecuted).isEqualTo(9);
        assertThat(flyway.migrate().migrationsExecuted).isZero();
        try(var connection=DriverManager.getConnection(url,user,password);var s=connection.createStatement()){
            try(var r=s.executeQuery("SELECT COUNT(*) FROM category_brands WHERE category_id=1 AND brand_id=1")){r.next();assertThat(r.getInt(1)).isEqualTo(1);}
            try(var r=s.executeQuery("SELECT scope_type FROM vouchers WHERE id=1")){r.next();assertThat(r.getString(1)).isEqualTo("ALL");}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM promotions WHERE scope_type='CATALOG'")){r.next();assertThat(r.getInt(1)).isEqualTo(2);}
            try(var r=s.executeQuery("SELECT p.discount_rate FROM promotions p JOIN product_promotion x ON x.promotion_id=p.id WHERE p.scope_type='PRODUCT' AND x.product_id=1")){r.next();assertThat(r.getDouble(1)).isEqualTo(7);}
            try(var r=s.executeQuery("SELECT variant_id,variant_label FROM order_details WHERE id=1")){assertThat(r.next()).isTrue();assertThat(r.getObject(1)).isNull();assertThat(r.getString(2)).isNull();}
            try(var r=s.executeQuery("SELECT COUNT(*) FROM product_variants")){assertThat(r.next()).isTrue();assertThat(r.getLong(1)).isZero();}
            try(var r=s.executeQuery("SELECT name,quantity,technology_catalog,sku FROM products WHERE id=1")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("Legacy food product");assertThat(r.getLong(2)).isEqualTo(9);assertThat(r.getBoolean(3)).isFalse();assertThat(r.getString(4)).isEqualTo("LEGACY-1");}
            try(var r=s.executeQuery("SELECT d.product_name,d.price,o.total_price FROM order_details d JOIN orders o ON o.id=d.order_id WHERE d.id=1")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("Legacy food product");assertThat(r.getDouble(2)).isEqualTo(25000);assertThat(r.getDouble(3)).isEqualTo(25000);}
            try(var r=s.executeQuery("SELECT code,active,minimum_order,used_count,usage_limit FROM vouchers WHERE id=1")){assertThat(r.next()).isTrue();assertThat(r.getString(1)).isEqualTo("LEGACY10");assertThat(r.getBoolean(2)).isTrue();assertThat(r.getDouble(3)).isZero();assertThat(r.getLong(4)).isZero();assertThat(r.getObject(5)).isNull();}
        }
    }
}
