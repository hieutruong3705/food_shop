package com.example.shop.api.service;

import com.example.shop.domain.Brand;
import com.example.shop.domain.Category;
import com.example.shop.domain.Product;
import com.example.shop.domain.ScopedOffer;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;

/** Words for an offer's scope, shared by the storefront offers list and the admin lists. */
public final class OfferScopeText {
    private static final int SHOWN_PRODUCTS = 5;

    private OfferScopeText() {}

    /** Names of everything the scope lists: its categories or brands first, then its products. */
    public static List<String> names(ScopedOffer offer) {
        Stream<String> groups = switch (offer.getScopeType()) {
            case CATEGORY -> offer.getCategories().stream().map(Category::getName);
            case BRAND -> offer.getBrands().stream().map(Brand::getName);
            case ALL, PRODUCT -> Stream.empty();
        };
        return Stream.concat(groups, offer.getProducts().stream().map(Product::getName)).toList();
    }

    /** One readable sentence, e.g. "Danh mục Laptop · Sản phẩm Dell XPS 13 và 2 sản phẩm khác". */
    public static String describe(ScopedOffer offer) {
        var parts = new ArrayList<String>();
        switch (offer.getScopeType()) {
            case ALL -> parts.add("Tất cả sản phẩm");
            case CATEGORY -> {
                if (!offer.getCategories().isEmpty()) parts.add("Danh mục " + join(offer.getCategories().stream().map(Category::getName).sorted().toList()));
            }
            case BRAND -> {
                if (!offer.getBrands().isEmpty()) parts.add("Thương hiệu " + join(offer.getBrands().stream().map(Brand::getName).sorted().toList()));
            }
            case PRODUCT -> { }
        }
        if (!offer.getProducts().isEmpty()) {
            var names = offer.getProducts().stream().map(Product::getName).sorted().toList();
            var shown = names.subList(0, Math.min(SHOWN_PRODUCTS, names.size()));
            parts.add("Sản phẩm " + String.join(", ", shown) + (names.size() > SHOWN_PRODUCTS ? " và " + (names.size() - SHOWN_PRODUCTS) + " sản phẩm khác" : ""));
        }
        return String.join(" · ", parts);
    }

    private static String join(List<String> names) {
        return String.join(", ", names);
    }
}
