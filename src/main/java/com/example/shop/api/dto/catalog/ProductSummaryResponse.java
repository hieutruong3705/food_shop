package com.example.shop.api.dto.catalog;

import com.example.shop.domain.Product;

public record ProductSummaryResponse(long id, String name, double price, double discountedPrice,
        String image, String shortDescription, long quantity, boolean active, String category, String brand) {
    public static ProductSummaryResponse from(Product product) {
        return new ProductSummaryResponse(product.getId(), product.getName(), product.getPrice(), product.getDiscountedPrice(),
                product.getImage(), product.getShortDesc(), product.getQuantity(), product.isActive(),
                product.getCategory() == null ? null : product.getCategory().getName(),
                product.getBrand() == null ? null : product.getBrand().getName());
    }
}
