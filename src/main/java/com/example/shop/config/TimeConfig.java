package com.example.shop.config;

import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;
import java.time.Clock;
import java.time.ZoneId;

/**
 * One notion of "now" and of wall-clock time for the shop.
 *
 * <p>Promotion windows are stored as wall-clock timestamps and the discounted price is evaluated by
 * the database ({@code current_timestamp}), while banners, vouchers and payment expiry use Java.
 * On MySQL the stored wall-clock time and the database session must therefore use the same zone,
 * whatever the host, container or JVM zone is. Java code should take the {@link Clock} bean.
 */
@Configuration
public class TimeConfig {
    public static final ZoneId SHOP_ZONE = ZoneId.of("Asia/Ho_Chi_Minh");
    /** Vietnam has no daylight saving time, so a fixed offset works without MySQL time zone tables. */
    static final String MYSQL_SESSION_ZONE_SQL = "SET time_zone = '+07:00'";

    @Bean
    public Clock clock() {
        return Clock.system(SHOP_ZONE);
    }

    /** Hibernate binds and reads timestamps as Vietnam wall-clock time instead of the JVM zone. */
    @Bean
    public HibernatePropertiesCustomizer shopJdbcTimeZone(Environment environment) {
        return properties -> {
            if (isMySql(environment.getProperty("spring.datasource.url"))) {
                properties.put("hibernate.jdbc.time_zone", SHOP_ZONE.getId());
            }
        };
    }

    /** Pins every MySQL/MariaDB connection to the same zone, whatever the server default is. */
    @Bean
    public static BeanPostProcessor mysqlSessionTimeZone() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof HikariDataSource pool && pool.getConnectionInitSql() == null && isMySql(pool.getJdbcUrl())) {
                    pool.setConnectionInitSql(MYSQL_SESSION_ZONE_SQL);
                }
                return bean;
            }
        };
    }

    static boolean isMySql(String jdbcUrl) {
        return jdbcUrl != null && (jdbcUrl.startsWith("jdbc:mysql:") || jdbcUrl.startsWith("jdbc:mariadb:"));
    }
}
