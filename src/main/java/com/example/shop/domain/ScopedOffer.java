package com.example.shop.domain;
import java.util.List;
public interface ScopedOffer {
    ScopeType getScopeType();void setScopeType(ScopeType type);
    List<Category> getCategories();void setCategories(List<Category> values);
    List<Brand> getBrands();void setBrands(List<Brand> values);
    List<Product> getProducts();void setProducts(List<Product> values);
    default OfferScopeRule.Scope scope(){return OfferScopeRule.Scope.of(this);}
    default boolean appliesTo(Product product){return scope().matches(product);}
}
