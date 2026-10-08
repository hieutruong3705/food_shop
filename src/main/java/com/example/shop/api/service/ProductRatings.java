package com.example.shop.api.service;

import com.example.shop.domain.Product;
import com.example.shop.repository.ReviewRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.*;

/** Star ratings for a page of product cards, looked up in one query instead of one per product. */
@Service
@Transactional(readOnly = true)
public class ProductRatings {
    /** Average rounded to one decimal, as the cards show it; count is how many reviews it came from. */
    public record Summary(long count, double average) {}
    private static final Summary NONE = new Summary(0, 0);
    private final ReviewRepository reviews;
    public ProductRatings(ReviewRepository reviews) { this.reviews = reviews; }

    public Map<Long, Summary> of(Collection<Product> products) {
        var ids = products.stream().map(Product::getId).distinct().toList();
        if (ids.isEmpty()) return Map.of();
        var result = new HashMap<Long, Summary>();
        for (Object[] row : reviews.ratingsFor(ids)) {
            double average = Math.round(((Number) row[2]).doubleValue() * 10) / 10.0;
            result.put(((Number) row[0]).longValue(), new Summary(((Number) row[1]).longValue(), average));
        }
        return result;
    }
    public static Summary none() { return NONE; }
}
