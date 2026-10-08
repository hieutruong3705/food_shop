package com.example.shop.api.dto.catalog;

import com.example.shop.api.service.ProductRatings;
import com.example.shop.api.service.PromotionPricing.Discounts;
import com.example.shop.api.service.PromotionPricing.Sale;
import com.example.shop.domain.Product;
import java.util.Date;
import java.util.Map;

/**
 * {@code discountRate}, {@code promotionName} and {@code promotionEndsAt} describe the running
 * promotion behind {@code discountedPrice}; they are 0 and null when the product is not on sale.
 */
public record ProductSummaryResponse(long id, String name, double price, double discountedPrice,
        String image, String shortDescription, long quantity, boolean active, String category, String brand,
        double discountRate, String promotionName, Date promotionEndsAt,
        Integer warrantyMonths, double ratingAverage, long ratingCount) {
    /** Without ratings: the card then shows no stars, which is what an unrated product looks like. */
    public static ProductSummaryResponse from(Product product, Discounts discounts) {
        return from(product, discounts, Map.of());
    }
    public static ProductSummaryResponse from(Product product, Discounts discounts, Map<Long, ProductRatings.Summary> ratings) {
        Sale sale = discounts.sale(product);
        ProductRatings.Summary rating = ratings.getOrDefault(product.getId(), ProductRatings.none());
        return new ProductSummaryResponse(product.getId(), product.getName(), product.getPrice(), discounts.price(product, product.getPrice()),
                product.getImage(), product.getShortDesc(), product.getQuantity(), product.isActive(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getBrand() == null ? null : product.getBrand().getName(),
                sale == null ? 0 : sale.rate(), sale == null ? null : sale.name(), sale == null ? null : sale.endDate(),
                product.getWarrantyMonths(), rating.average(), rating.count());
    }
}
