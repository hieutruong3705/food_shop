package com.example.shop.repository;
import com.example.shop.domain.*;
import org.springframework.data.jpa.domain.Specification;
public final class OfferScopeSpecification {
    private OfferScopeSpecification(){}
    public static Specification<Product> visible(ScopedOffer offer){return (root,q,cb)->{
        var scope=switch(offer.getScopeType()){
            case ALL -> cb.conjunction();
            case CATEGORY -> root.get("category").get("id").in(offer.getCategories().stream().map(Category::getId).toList());
            case BRAND -> root.get("brand").get("id").in(offer.getBrands().stream().map(Brand::getId).toList());
            case PRODUCT -> root.get("id").in(offer.getProducts().stream().map(Product::getId).toList());
        };
        return cb.and(cb.isTrue(root.get("active")),cb.isTrue(root.get("technologyCatalog")),scope);
    };}
}
