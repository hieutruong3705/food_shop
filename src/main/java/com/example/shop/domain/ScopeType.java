package com.example.shop.domain;

/**
 * Which products an offer (promotion or voucher) applies to. The types never mix: an offer is
 * either for the whole store, for categories, for brands, or for hand-picked products.
 *
 * <ul>
 *   <li>{@code ALL}: every product.</li>
 *   <li>{@code CATEGORY}: every product of the listed categories, plus the listed products. A
 *       category the admin opened up and picked products from is not listed itself; only those
 *       products are.</li>
 *   <li>{@code BRAND}: the same, by brand.</li>
 *   <li>{@code PRODUCT}: exactly the listed products.</li>
 * </ul>
 */
public enum ScopeType {
    ALL, CATEGORY, BRAND, PRODUCT;

    /** Reads the scope names of the API. {@code CATALOG}, the short-lived combined type, is not one of them. */
    public static ScopeType parse(String value) {
        return switch (value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT)) {
            case "ALL" -> ALL;
            case "CATEGORY" -> CATEGORY;
            case "BRAND" -> BRAND;
            case "PRODUCT" -> PRODUCT;
            default -> throw new IllegalArgumentException("Phạm vi áp dụng không hợp lệ; chọn ALL, CATEGORY, BRAND hoặc PRODUCT");
        };
    }
}
