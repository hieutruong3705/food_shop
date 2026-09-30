package com.example.shop;

import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;
import java.util.stream.StreamSupport;
import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class HomeApiTests {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired EntityManager em;
    @Autowired CategoryRepository categories;
    @Autowired BrandRepository brands;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;
    @Autowired OrderDetailRepository lines;
    @Autowired PromotionRepository promotions;
    @Autowired ReviewRepository reviews;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;

    JsonNode home() throws Exception {
        return json.readTree(mvc.perform(get("/api/v1/home")).andExpect(status().isOk()).andReturn().getResponse().getContentAsString());
    }

    static List<JsonNode> list(JsonNode array) {
        return StreamSupport.stream(array.spliterator(), false).toList();
    }

    @Test
    void homeIsPublicAndAlwaysHasEveryBlock() throws Exception {
        var body = home();
        for (String key : List.of("promotion", "brands", "bestSellers", "categoryRows", "reviews")) assertThat(body.has(key)).as(key).isTrue();
        assertThat(body.path("brands").isArray()).isTrue();
        assertThat(body.path("bestSellers").isArray()).isTrue();
        assertThat(body.path("categoryRows").isArray()).isTrue();
        assertThat(body.path("reviews").isArray()).isTrue();
    }

    @Test
    void blocksCarryTheRunningPromotionBrandsBestSellersBusiestCategoryAndGoodReviews() throws Exception {
        String key = UUID.randomUUID().toString();
        var brand = new Brand(); brand.setName(key + " brand"); brand = brands.save(brand);
        var category = new Category(); category.setName(key + " category"); category = categories.save(category);
        var shown = new ArrayList<Product>();
        for (int i = 0; i < 8; i++) shown.add(product(category, brand, key + " p" + i, 3));
        var hidden = product(category, brand, key + " hidden", 3); hidden.setActive(false);
        var emptyBrand = new Brand(); emptyBrand.setName(key + " empty brand"); brands.save(emptyBrand);

        var promotion = new Promotion();
        promotion.setName(key); promotion.setDiscountRate(100); promotion.setScopeType(ScopeType.BRAND); promotion.setBrands(new ArrayList<>(List.of(brand)));
        promotion.setStartDate(new Date(System.currentTimeMillis() - 10000)); promotion.setEndDate(new Date(System.currentTimeMillis() + 100000));
        promotions.save(promotion);

        var order = new Order(); order.setStatus(OrderStatus.COMPLETED); order.setPaymentMethod("COD"); order.setPaymentStatus(PaymentStatus.PAID); orders.save(order);
        var detail = new OrderDetail(); detail.setOrder(order); detail.setProduct(shown.get(2)); detail.setQuantity(7); detail.setPrice(1000000); lines.save(detail);

        var role = roles.findByName("USER");
        if (role == null) { role = new Role(); role.setName("USER"); role = roles.save(role); }
        var author = new User(); author.setEmail(key + "@home.test"); author.setFullName("Nguyễn Văn " + key.substring(0, 4)); author.setEnabled(true); author.setRole(role); users.save(author);
        var good = review(author, shown.get(0), 5, "Rất tốt " + key);
        review(author, shown.get(1), 2, "Tạm " + key);
        em.flush(); em.clear();

        var body = home();
        assertThat(body.path("promotion").path("name").asText()).isEqualTo(key);
        assertThat(body.path("promotion").path("products")).hasSize(8);
        assertThat(body.path("promotion").path("scopeDescription").asText()).contains(brand.getName());
        assertThat(list(body.path("promotion").path("products"))).allMatch(p -> p.path("discountRate").asDouble() == 100);

        var brandRows = list(body.path("brands"));
        assertThat(brandRows).anyMatch(b -> b.path("name").asText().equals(key + " brand") && b.path("count").asLong() == 8);
        assertThat(brandRows).noneMatch(b -> b.path("name").asText().equals(key + " empty brand"));

        var seller = list(body.path("bestSellers")).stream().filter(row -> row.path("product").path("id").asLong() == shown.get(2).getId()).findFirst();
        assertThat(seller).isPresent();
        assertThat(seller.get().path("units").asLong()).isEqualTo(7);

        var row = list(body.path("categoryRows")).stream().filter(r -> r.path("category").path("name").asText().equals(key + " category")).findFirst();
        assertThat(row).isPresent();
        assertThat(row.get().path("category").path("productCount").asLong()).isEqualTo(8);
        assertThat(row.get().path("products")).hasSize(4);

        var quotes = list(body.path("reviews"));
        assertThat(quotes).anyMatch(r -> r.path("content").asText().equals("Rất tốt " + key));
        assertThat(quotes).noneMatch(r -> r.path("content").asText().equals("Tạm " + key));
        var shownReview = quotes.stream().filter(r -> r.path("content").asText().equals("Rất tốt " + key)).findFirst().orElseThrow();
        assertThat(shownReview.path("productName").asText()).isEqualTo(shown.get(0).getName());
        assertThat(shownReview.has("email")).isFalse();
        assertThat(good.getRating()).isEqualTo(5);
    }

    private Product product(Category category, Brand brand, String name, long stock) {
        var p = new Product();
        p.setName(name); p.setSku(UUID.randomUUID().toString()); p.setCategory(category); p.setBrand(brand); p.setPrice(1000000); p.setQuantity(stock);
        return products.save(p);
    }

    private Review review(User user, Product product, int rating, String content) {
        var r = new Review();
        r.setUser(user); r.setProduct(product); r.setRating(rating); r.setContent(content);
        return reviews.save(r);
    }
}
