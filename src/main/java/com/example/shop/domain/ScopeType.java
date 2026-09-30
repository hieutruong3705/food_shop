package com.example.shop.domain;

/**
 * Which products an offer (promotion or voucher) applies to.
 *
 * <ul>
 *   <li>{@code ALL}: every product.</li>
 *   <li>{@code CATALOG}: products of the chosen categories AND of the chosen brands. Only categories
 *       means the whole category, only brands means the whole brand; both together narrow it down
 *       (Laptop + Dell + HP is the laptops made by Dell or HP).</li>
 *   <li>{@code PRODUCT}: exactly the chosen products.</li>
 * </ul>
 */
public enum ScopeType {
    ALL, CATALOG, PRODUCT;

    /**
     * Reads the scope names of the API and of data written before CATALOG existed. The old
     * CATEGORY and BRAND scopes are CATALOG scopes that list only categories or only brands.
     */
    public static ScopeType parse(String value) {
        return switch (value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ALL" -> ALL;
            case "CATALOG", "CATEGORY", "BRAND" -> CATALOG;
            case "PRODUCT" -> PRODUCT;
            default -> throw new IllegalArgumentException("Phạm vi áp dụng không hợp lệ; chọn ALL, CATALOG hoặc PRODUCT");
        };
    }
}
