package com.example.shop;

import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;

/**
 * A promotion window must mean the same instant on every read path: the discounted price
 * (evaluated in the database), the public /offers banner and the promotionId product filter
 * (evaluated in Java). Committed data is used on purpose so the database session, not just
 * the persistence context, evaluates the window. Run against MySQL with a UTC session
 * (default-time-zone='+00:00') to reproduce the Docker deployment.
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class PromotionClockTests {
    private static final double PRICE = 1000000;
    private static final double RATE = 10;

    @Autowired com.example.shop.api.service.PromotionPricing pricing;
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired BrandRepository brands;
    @Autowired CategoryRepository categories;
    @Autowired ProductRepository products;
    @Autowired PromotionRepository promotions;

    private final List<Runnable> cleanup = new ArrayList<>();

    @AfterEach
    void removeCommittedRows() {
        for (int i = cleanup.size() - 1; i >= 0; i--) {
            cleanup.get(i).run();
        }
    }

    private record Scenario(String name, Duration start, Duration end, boolean effective) {}

    private Product product(String key) {
        var brand = new Brand();
        brand.setName(key + "-brand");
        brand = brands.save(brand);
        var category = new Category();
        category.setName(key + "-category");
        category.setBrands(new ArrayList<>(List.of(brand)));
        category = categories.save(category);
        var product = new Product();
        product.setName(key + "-product");
        product.setBrand(brand);
        product.setCategory(category);
        product.setPrice(PRICE);
        product.setQuantity(5);
        product = products.save(product);
        long productId = product.getId();
        long categoryId = category.getId();
        long brandId = brand.getId();
        // Cleanup runs in reverse: the product first, then the category that references the brand.
        cleanup.add(() -> brands.deleteById(brandId));
        cleanup.add(() -> categories.deleteById(categoryId));
        cleanup.add(() -> products.deleteById(productId));
        return product;
    }

    private Promotion promotion(Product product, Scenario scenario) {
        var promotion = new Promotion();
        promotion.setName(scenario.name());
        promotion.setDiscountRate(RATE);
        promotion.setActive(true);
        promotion.setScopeType(ScopeType.PRODUCT);
        promotion.setProducts(new ArrayList<>(List.of(product)));
        var now = Instant.now();
        promotion.setStartDate(Date.from(now.plus(scenario.start())));
        promotion.setEndDate(Date.from(now.plus(scenario.end())));
        promotion = promotions.save(promotion);
        long promotionId = promotion.getId();
        cleanup.add(() -> promotions.deleteById(promotionId));
        return promotion;
    }

    @Test
    void everyReadPathAgreesWhetherAPromotionIsInEffect() throws Exception {
        var scenarios = List.of(
                // Started a minute ago: the 00:01 case of a promotion that begins at midnight.
                new Scenario("just-started", Duration.ofMinutes(-1), Duration.ofHours(1), true),
                new Scenario("running-since-hours", Duration.ofHours(-8), Duration.ofHours(8), true),
                // Ended three hours ago: a UTC database session would still see it as running.
                new Scenario("expired-three-hours-ago", Duration.ofHours(-5), Duration.ofHours(-3), false),
                new Scenario("starts-in-three-hours", Duration.ofHours(3), Duration.ofHours(5), false));

        for (var scenario : scenarios) {
            var key = "clock-" + UUID.randomUUID();
            var product = product(key);
            var promotion = promotion(product, scenario);

            double expectedPrice = scenario.effective() ? PRICE * (1 - RATE / 100) : PRICE;
            assertThat(pricing.snapshot().price(products.findById(product.getId()).orElseThrow(), products.findById(product.getId()).orElseThrow().getPrice()))
                    .as("discounted price for %s", scenario.name()).isEqualTo(expectedPrice);

            var offers = json.readTree(mvc.perform(get("/api/v1/offers")).andReturn().getResponse().getContentAsString());
            boolean listed = false;
            for (var offer : offers.path("promotions")) {
                listed |= offer.path("id").asLong() == promotion.getId();
            }
            assertThat(listed).as("/offers lists %s", scenario.name()).isEqualTo(scenario.effective());

            var filtered = json.readTree(mvc.perform(get("/api/v1/products")
                    .param("promotionId", String.valueOf(promotion.getId()))).andReturn().getResponse().getContentAsString());
            assertThat(filtered.path("totalElements").asLong())
                    .as("promotionId filter for %s", scenario.name()).isEqualTo(scenario.effective() ? 1 : 0);
        }
    }
}
