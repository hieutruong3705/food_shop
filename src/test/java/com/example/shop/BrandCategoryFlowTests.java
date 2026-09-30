package com.example.shop;

import com.example.shop.api.service.PromotionPricing;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** The brand ↔ category matrix of docs/PLAN-UI-REVIEW.md section 5, rows 8–13 and 15. */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class BrandCategoryFlowTests {
    @Autowired MockMvc mvc;
    @Autowired EntityManager em;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;
    @Autowired PromotionRepository promotions;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired PromotionPricing pricing;
    String key, admin;
    Brand dell;
    Category laptops, monitors;

    @BeforeEach
    void fixture() {
        key = UUID.randomUUID().toString();
        var role = roles.findByName("ADMIN");
        if (role == null) { role = new Role(); role.setName("ADMIN"); role = roles.save(role); }
        var u = new User(); admin = key + "@flow.test"; u.setEmail(admin); u.setEnabled(true); u.setRole(role); users.save(u);
        dell = brand("Dell");
        laptops = category("Laptop", dell);
        monitors = category("Monitor", dell);
    }

    Brand brand(String name) { var b = new Brand(); b.setName(key + name); return brands.save(b); }
    Category category(String name, Brand... assigned) { var c = new Category(); c.setName(key + name); c.setBrands(new ArrayList<>(List.of(assigned))); return categories.save(c); }
    Product product(Category c, Brand b) {
        var p = new Product(); p.setName(key + " " + UUID.randomUUID()); p.setSku(UUID.randomUUID().toString());
        p.setCategory(c); p.setBrand(b); p.setPrice(1000000); p.setQuantity(3); return products.save(p);
    }
    Promotion running(String name, double rate, ScopeType type) {
        var p = new Promotion(); p.setName(key + name); p.setDiscountRate(rate); p.setScopeType(type);
        p.setStartDate(new Date(System.currentTimeMillis() - 60000)); p.setEndDate(new Date(System.currentTimeMillis() + 600000));
        return p;
    }
    double price(Product p) { return pricing.snapshot().price(products.findById(p.getId()).orElseThrow(), p.getPrice()); }

    @Test
    void row8DeletingABrandThatStillHasProductsIsRefusedWithTheirNames() throws Exception {
        var loose = brand("Loose");
        var c = category("Loose category", loose);
        var p = product(c, loose);
        c.getBrands().clear(); em.flush(); em.clear();
        mvc.perform(delete("/api/v1/admin/brands/" + loose.getId()).with(user(admin).roles("ADMIN")).with(csrf()))
                .andExpect(status().isBadRequest()).andExpect(content().string(org.hamcrest.Matchers.containsString(p.getName())));
        assertThat(brands.findById(loose.getId())).isPresent();
    }

    /** Decision: an assigned brand with nothing on sale stays visible with count 0, in the filter and in the menu. */
    @Test
    void row9ABrandWhoseProductsAreAllStoppedStaysVisibleWithZero() throws Exception {
        var p = product(laptops, dell); p.setActive(false); em.flush(); em.clear();
        mvc.perform(get("/api/v1/catalog/facets").param("categoryId", "" + laptops.getId()))
                .andExpect(jsonPath("$.brands[0].id").value(dell.getId())).andExpect(jsonPath("$.brands[0].count").value(0));
        mvc.perform(get("/api/v1/catalog/menu").param("categoryId", "" + laptops.getId()))
                .andExpect(jsonPath("$.brands[0].id").value(dell.getId())).andExpect(jsonPath("$.brands[0].count").value(0))
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void row10RenamingAGroupKeepsItsOffers() {
        var p = product(laptops, dell);
        var offer = running("Rename", 10, ScopeType.CATEGORY); offer.setCategories(new ArrayList<>(List.of(laptops))); promotions.save(offer);
        em.flush();
        laptops.setName(key + "Laptop renamed"); dell.setName(key + "Dell renamed"); em.flush(); em.clear();
        assertThat(price(p)).isEqualTo(900000);
    }

    @Test
    void row11ABrandPromotionCoversProductsAddedLater() {
        var offer = running("Brand", 20, ScopeType.BRAND); offer.setBrands(new ArrayList<>(List.of(dell))); promotions.save(offer);
        em.flush();
        var added = product(monitors, dell); em.flush(); em.clear();
        assertThat(price(added)).isEqualTo(800000);
    }

    @Test
    void row12MovingAProductOutOfTheCategoryEndsItsCategoryDiscount() {
        var p = product(laptops, dell);
        var offer = running("Category", 10, ScopeType.CATEGORY); offer.setCategories(new ArrayList<>(List.of(laptops))); promotions.save(offer);
        em.flush();
        assertThat(price(p)).isEqualTo(900000);
        var moved = products.findById(p.getId()).orElseThrow(); moved.setCategory(monitors); em.flush(); em.clear();
        assertThat(price(p)).isEqualTo(1000000);
    }

    @Test
    void row13OverlappingPromotionsGiveTheHighestRateOnly() {
        var p = product(laptops, dell);
        var byBrand = running("Low", 10, ScopeType.BRAND); byBrand.setBrands(new ArrayList<>(List.of(dell))); promotions.save(byBrand);
        var byProduct = running("High", 25, ScopeType.PRODUCT); byProduct.setProducts(new ArrayList<>(List.of(p))); promotions.save(byProduct);
        em.flush(); em.clear();
        var sale = pricing.snapshot().sale(products.findById(p.getId()).orElseThrow());
        assertThat(sale.rate()).isEqualTo(25);
        assertThat(sale.name()).isEqualTo(key + "High");
        assertThat(price(p)).isEqualTo(750000);
    }

    @Test
    void savingAnOverlappingPromotionWarnsWithoutBlocking() throws Exception {
        product(laptops, dell);
        var existing = running("Existing", 10, ScopeType.BRAND); existing.setBrands(new ArrayList<>(List.of(dell))); promotions.save(existing);
        em.flush();
        String start = java.time.Instant.now().minusSeconds(30).toString(), end = java.time.Instant.now().plusSeconds(300).toString();
        var body = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("name", key + "New", "discountRate", 20, "active", true,
                "scopeType", "CATEGORY", "categoryIds", List.of(laptops.getId()), "startDate", start, "endDate", end));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/promotions").with(user(admin).roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(body))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.warnings.length()").value(1))
                .andExpect(jsonPath("$.warnings[0]").value(org.hamcrest.Matchers.containsString(key + "Existing")));
        var apart = new com.fasterxml.jackson.databind.ObjectMapper().writeValueAsString(Map.of("name", key + "Later", "discountRate", 20, "active", true,
                "scopeType", "CATEGORY", "categoryIds", List.of(laptops.getId()),
                "startDate", java.time.Instant.now().plusSeconds(86400).toString(), "endDate", java.time.Instant.now().plusSeconds(90000).toString()));
        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post("/api/v1/admin/promotions").with(user(admin).roles("ADMIN")).with(csrf())
                        .contentType("application/json").content(apart))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.warnings.length()").value(0));
    }

    @Test
    void publicCategoryCountsOnlyWhatAShopperCanBuy() throws Exception {
        product(laptops, dell);
        var stopped = product(laptops, dell); stopped.setActive(false);
        var hidden = product(laptops, dell); hidden.setTechnologyCatalog(false);
        product(monitors, dell).setActive(false);
        em.flush(); em.clear();
        var body = new com.fasterxml.jackson.databind.ObjectMapper().readTree(mvc.perform(get("/api/v1/categories")).andReturn().getResponse().getContentAsString());
        var rows = new ArrayList<com.fasterxml.jackson.databind.JsonNode>(); body.forEach(rows::add);
        assertThat(rows).filteredOn(r -> r.path("id").asLong() == laptops.getId()).singleElement().satisfies(r -> assertThat(r.path("productCount").asLong()).isEqualTo(1));
        assertThat(rows).noneMatch(r -> r.path("id").asLong() == monitors.getId());
        // Deleting still sees the stopped products.
        mvc.perform(delete("/api/v1/admin/categories/" + monitors.getId()).with(user(admin).roles("ADMIN")).with(csrf())).andExpect(status().isBadRequest());
    }

    @Test
    void unknownPagesServeTheWebAppWith404AndApiKeepsJson() throws Exception {
        mvc.perform(get("/khong-ton-tai").accept(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(status().isNotFound()).andExpect(content().contentTypeCompatibleWith(org.springframework.http.MediaType.TEXT_HTML))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("<div id=\"app\">")));
        mvc.perform(get("/api/v1/khong-ton-tai").with(user(admin).roles("ADMIN")).accept(org.springframework.http.MediaType.TEXT_HTML, org.springframework.http.MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound()).andExpect(jsonPath("$.message").exists());
        mvc.perform(get("/images/khong-ton-tai.png").accept(org.springframework.http.MediaType.TEXT_HTML)).andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.not(org.hamcrest.Matchers.containsString("<div id=\"app\">"))));
    }

    @Test
    void searchMatchesNameSkuBrandAndCategory() throws Exception {
        var p = product(laptops, dell);
        var other = brand("Other"); var shelf = category("Shelf", other);
        var q = product(shelf, other);
        em.flush(); em.clear();
        for (String term : List.of(p.getName().substring(key.length() + 1), p.getSku().substring(0, 13).toUpperCase(Locale.ROOT), key + "Dell", key + "Laptop")) {
            mvc.perform(get("/api/v1/products").param("search", term).param("size", "60"))
                    .andExpect(status().isOk()).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].id").value(p.getId()));
        }
        mvc.perform(get("/api/v1/products").param("search", key).param("size", "60")).andExpect(jsonPath("$.totalElements").value(2));
        assertThat(q.getId()).isNotEqualTo(p.getId());
    }

    @Test
    void row15AMenuCategoryWithoutBrandsSendsAnEmptyList() throws Exception {
        var bare = category("Bare");
        em.flush(); em.clear();
        mvc.perform(get("/api/v1/catalog/menu").param("categoryId", "" + bare.getId()))
                .andExpect(status().isOk()).andExpect(jsonPath("$.brands.length()").value(0))
                .andExpect(jsonPath("$.bestSellers.length()").value(0)).andExpect(jsonPath("$.newest.length()").value(0));
    }
}
