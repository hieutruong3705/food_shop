package com.example.shop.api.service;

import com.example.shop.api.dto.catalog.ProductSummaryResponse;
import com.example.shop.domain.Brand;
import com.example.shop.domain.Category;
import com.example.shop.domain.Product;
import com.example.shop.domain.Promotion;
import com.example.shop.domain.Review;
import com.example.shop.repository.BrandRepository;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.OfferScopeSpecification;
import com.example.shop.repository.OrderDetailRepository;
import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.PromotionRepository;
import com.example.shop.repository.ReviewRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import static com.example.shop.api.service.AdminSupport.map;

/**
 * Everything the storefront home page shows below its banner, in one call: the running promotion
 * with its products, brands, best sellers, one row per busy category and recent good reviews.
 * A block with nothing to show is empty (or null), never filled with placeholders. The answer is
 * kept for {@code shop.home-cache-seconds} so a busy home page costs a few queries a minute.
 */
@Service
@Transactional(readOnly = true)
public class HomeReadService {
    private static final int PROMOTION_PRODUCTS = 8;
    private static final int BEST_SELLERS = 8;
    private static final int BRANDS = 12;
    private static final int CATEGORY_ROWS = 3;
    private static final int CATEGORY_PRODUCTS = 4;
    private static final int REVIEWS = 4;

    private record Cached(Instant at, Map<String, Object> body) {}

    private final PromotionRepository promotions;
    private final ProductRepository products;
    private final BrandRepository brands;
    private final CategoryRepository categories;
    private final OrderDetailRepository orderDetails;
    private final ReviewRepository reviews;
    private final PromotionPricing pricing;
    private final ProductRatings ratings;
    private final Clock clock;
    private final long cacheSeconds;
    private volatile Cached cached;

    public HomeReadService(PromotionRepository promotions, ProductRepository products, BrandRepository brands, CategoryRepository categories,
            OrderDetailRepository orderDetails, ReviewRepository reviews, PromotionPricing pricing, ProductRatings ratings, Clock clock,
            @Value("${shop.home-cache-seconds:30}") long cacheSeconds) {
        this.promotions = promotions;
        this.products = products;
        this.brands = brands;
        this.categories = categories;
        this.orderDetails = orderDetails;
        this.reviews = reviews;
        this.pricing = pricing;
        this.ratings = ratings;
        this.clock = clock;
        this.cacheSeconds = cacheSeconds;
    }

    public Map<String, Object> home() {
        Cached current = cached;
        if (cacheSeconds > 0 && current != null && current.at().plusSeconds(cacheSeconds).isAfter(clock.instant())) return current.body();
        Map<String, Object> body = build();
        if (cacheSeconds > 0) cached = new Cached(clock.instant(), body);
        return body;
    }

    private Map<String, Object> build() {
        var discounts = pricing.current();
        var result = new LinkedHashMap<String, Object>();
        result.put("promotion", promotionRow(discounts));
        result.put("brands", brandStrip());
        result.put("bestSellers", bestSellers(discounts));
        result.put("categoryRows", categoryRows(discounts));
        result.put("reviews", featuredReviews());
        return result;
    }

    /** The running promotion with the highest rate that still has products a shopper can buy. */
    private Map<String, Object> promotionRow(PromotionPricing.Discounts discounts) {
        var running = new ArrayList<>(promotions.findRunning(Date.from(clock.instant())));
        running.sort(Comparator.comparingDouble(Promotion::getDiscountRate).reversed().thenComparing(Comparator.comparingLong(Promotion::getId).reversed()));
        for (Promotion promotion : running) {
            if (promotion.scope().isEmpty()) continue;
            var shown = latest(OfferScopeSpecification.visible(promotion), PROMOTION_PRODUCTS);
            if (shown.isEmpty()) continue;
            return map("id", promotion.getId(), "name", promotion.getName(), "description", promotion.getDescription(),
                    "imageUrl", promotion.getImageUrl() == null ? "" : promotion.getImageUrl(),
                    "discountRate", promotion.getDiscountRate(), "endDate", promotion.getEndDate(),
                    "scopeDescription", OfferScopeText.describe(promotion),
                    "products", shown.stream().map(p -> ProductSummaryResponse.from(p, discounts, ratings.of(shown))).toList());
        }
        return null;
    }

    private List<Map<String, Object>> brandStrip() {
        var all = brands.findAll();
        var counts = counts(products.sellableCountsByBrand(all.isEmpty() ? List.of(-1L) : all.stream().map(Brand::getId).toList()));
        return all.stream().filter(b -> counts.getOrDefault(b.getId(), 0L) > 0)
                .sorted(Comparator.comparingLong((Brand b) -> counts.get(b.getId())).reversed().thenComparing(Brand::getName))
                .limit(BRANDS)
                .map(b -> map("id", b.getId(), "name", b.getName(), "logoUrl", b.getLogoUrl() == null ? "" : b.getLogoUrl(), "count", counts.get(b.getId())))
                .toList();
    }

    /** Ranked like the category menu and the popular sort: paid or cash-on-delivery units in confirmed orders. */
    private List<Map<String, Object>> bestSellers(PromotionPricing.Discounts discounts) {
        var ranking = orderDetails.topSalesOverall(PageRequest.of(0, BEST_SELLERS));
        var byId = products.findAllById(ranking.stream().map(r -> r.getProductId()).toList()).stream().collect(Collectors.toMap(Product::getId, p -> p));
        var scores = ratings.of(byId.values());
        var rows = new ArrayList<Map<String, Object>>();
        for (var rank : ranking) {
            Product p = byId.get(rank.getProductId());
            if (p != null) rows.add(map("product", ProductSummaryResponse.from(p, discounts, scores), "units", rank.getUnits()));
        }
        return rows;
    }

    private List<Map<String, Object>> categoryRows(PromotionPricing.Discounts discounts) {
        var all = categories.findAll();
        var counts = counts(products.sellableCountsByCategory(all.isEmpty() ? List.of(-1L) : all.stream().map(Category::getId).toList()));
        var busiest = all.stream().filter(c -> counts.getOrDefault(c.getId(), 0L) > 0)
                .sorted(Comparator.comparingLong((Category c) -> counts.get(c.getId())).reversed().thenComparingLong(Category::getId))
                .limit(CATEGORY_ROWS).toList();
        var rows = new ArrayList<Map<String, Object>>();
        for (Category category : busiest) {
            Specification<Product> inCategory = (root, q, cb) -> cb.and(cb.isTrue(root.get("active")), cb.isTrue(root.get("technologyCatalog")),
                    cb.equal(root.get("category").get("id"), category.getId()));
            rows.add(map("category", map("id", category.getId(), "name", category.getName(), "image", category.getImage() == null ? "" : category.getImage(),
                            "productCount", counts.get(category.getId())),
                    "products", latest(inCategory, CATEGORY_PRODUCTS).stream().map(p -> ProductSummaryResponse.from(p, discounts)).toList()));
        }
        return rows;
    }

    private List<Map<String, Object>> featuredReviews() {
        var rows = new ArrayList<Map<String, Object>>();
        for (Review review : reviews.featured(PageRequest.of(0, REVIEWS))) {
            String author = review.getUser() == null || review.getUser().getFullName() == null || review.getUser().getFullName().isBlank()
                    ? "Khách hàng" : review.getUser().getFullName().trim();
            rows.add(map("rating", review.getRating(), "content", review.getContent(), "author", author,
                    "productId", review.getProduct().getId(), "productName", review.getProduct().getName(), "createdAt", review.getCreatedAt()));
        }
        return rows;
    }

    /** Products in stock first, then newest, the same order the catalog uses. */
    private List<Product> latest(Specification<Product> filter, int size) {
        return products.findAll((root, q, cb) -> {
            if (q.getResultType() != Long.class && q.getResultType() != long.class) {
                q.orderBy(cb.asc(cb.<Integer>selectCase().when(cb.gt(root.get("quantity"), 0), 0).otherwise(1)), cb.desc(root.get("id")));
            }
            return filter.toPredicate(root, q, cb);
        }, PageRequest.of(0, size)).getContent();
    }

    private static Map<Long, Long> counts(List<Object[]> rows) {
        var result = new HashMap<Long, Long>();
        for (Object[] row : rows) result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        return result;
    }
}
