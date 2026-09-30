package com.example.shop;

import com.example.shop.config.TimeConfig;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.example.shop.support.MovableClock;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * The Java-side offer paths take their "now" from the Clock bean. A promotion or voucher that begins
 * at 00:00 Vietnam time must be visible at 00:01 and hidden at 23:59 the day before. The discounted
 * price is still evaluated by the database clock, so it is deliberately not asserted here.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FixedClockTests {
    static final ZoneId ZONE = TimeConfig.SHOP_ZONE;
    static final ZonedDateTime MIDNIGHT = ZonedDateTime.of(2026, 10, 1, 0, 0, 0, 0, ZONE);

    @TestConfiguration
    static class ClockConfig {
        @Bean @Primary MovableClock movableClock() { return new MovableClock(MIDNIGHT.plusMinutes(1).toInstant()); }
    }

    @Autowired MovableClock clock;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired PromotionRepository promotions;
    @Autowired VoucherRepository vouchers;

    private final List<Runnable> cleanup = new ArrayList<>();

    @AfterEach
    void removeCommittedRows() {
        for (int i = cleanup.size() - 1; i >= 0; i--) {
            cleanup.get(i).run();
        }
    }

    @Test
    void offersAndPromotionFilterFollowTheClockAtTheMidnightBoundary() throws Exception {
        var brand = new Brand();
        brand.setName("clock-" + UUID.randomUUID());
        brand = brands.save(brand);
        var category = new Category();
        category.setName("clock-" + UUID.randomUUID());
        category.setBrands(new ArrayList<>(List.of(brand)));
        category = categories.save(category);
        var product = new Product();
        product.setName("clock-" + UUID.randomUUID());
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(1000000);
        product.setQuantity(5);
        product = products.save(product);
        long brandId = brand.getId();
        long categoryId = category.getId();
        long productId = product.getId();
        cleanup.add(() -> brands.deleteById(brandId));
        cleanup.add(() -> categories.deleteById(categoryId));
        cleanup.add(() -> products.deleteById(productId));

        var promotion = new Promotion();
        promotion.setName("midnight-sale");
        promotion.setDiscountRate(10);
        promotion.setActive(true);
        promotion.setScopeType(ScopeType.PRODUCT);
        promotion.setProducts(new ArrayList<>(List.of(product)));
        promotion.setStartDate(Date.from(MIDNIGHT.toInstant()));
        promotion.setEndDate(Date.from(MIDNIGHT.plusDays(1).toInstant()));
        promotion = promotions.save(promotion);
        long promotionId = promotion.getId();
        cleanup.add(() -> promotions.deleteById(promotionId));

        var voucher = new Voucher();
        voucher.setCode("MIDNIGHT" + UUID.randomUUID().toString().substring(0, 8).toUpperCase());
        voucher.setDiscount(10);
        voucher.setActive(true);
        voucher.setScopeType(ScopeType.PRODUCT);
        voucher.setProducts(new ArrayList<>(List.of(product)));
        voucher.setStartDate(Date.from(MIDNIGHT.toInstant()));
        voucher.setEndDate(Date.from(MIDNIGHT.plusDays(1).toInstant()));
        voucher = vouchers.save(voucher);
        long voucherId = voucher.getId();
        String voucherCode = voucher.getCode();
        cleanup.add(() -> vouchers.deleteById(voucherId));

        clock.set(MIDNIGHT.minusMinutes(1).toInstant());
        assertThat(listsPromotion(promotionId)).as("/offers promotion at 23:59").isFalse();
        assertThat(listsVoucher(voucherCode)).as("/offers voucher at 23:59").isFalse();
        assertThat(filteredCount(promotionId)).as("promotionId filter at 23:59").isZero();

        clock.set(MIDNIGHT.plusMinutes(1).toInstant());
        assertThat(listsPromotion(promotionId)).as("/offers promotion at 00:01").isTrue();
        assertThat(listsVoucher(voucherCode)).as("/offers voucher at 00:01").isTrue();
        assertThat(filteredCount(promotionId)).as("promotionId filter at 00:01").isEqualTo(1);

        clock.set(MIDNIGHT.plusDays(1).plusMinutes(1).toInstant());
        assertThat(listsPromotion(promotionId)).as("/offers promotion after the end").isFalse();
        assertThat(listsVoucher(voucherCode)).as("/offers voucher after the end").isFalse();
        assertThat(filteredCount(promotionId)).as("promotionId filter after the end").isZero();
    }

    private JsonNode offers() throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/offers")).andReturn().getResponse().getContentAsString());
    }

    private boolean listsPromotion(long id) throws Exception {
        for (var offer : offers().path("promotions")) {
            if (offer.path("id").asLong() == id) {
                return true;
            }
        }
        return false;
    }

    private boolean listsVoucher(String code) throws Exception {
        for (var offer : offers().path("vouchers")) {
            if (code.equals(offer.path("code").asText())) {
                return true;
            }
        }
        return false;
    }

    private long filteredCount(long promotionId) throws Exception {
        var page = json.readTree(mvc.perform(get("/api/v1/products").param("promotionId", String.valueOf(promotionId)))
                .andReturn().getResponse().getContentAsString());
        return page.path("totalElements").asLong();
    }
}
