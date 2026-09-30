package com.example.shop.domain;
import java.util.List;
public interface ScopedOffer {
    ScopeType getScopeType();void setScopeType(ScopeType type);
    List<Category> getCategories();void setCategories(List<Category> values);
    List<Brand> getBrands();void setBrands(List<Brand> values);
    List<Product> getProducts();void setProducts(List<Product> values);
    default boolean appliesTo(Product product){
        return switch(getScopeType()){
            case ALL -> true;
            case CATEGORY -> product.getCategory()!=null&&getCategories().stream().anyMatch(c->c.getId()==product.getCategory().getId());
            case BRAND -> product.getBrand()!=null&&getBrands().stream().anyMatch(b->b.getId()==product.getBrand().getId());
            case PRODUCT -> getProducts().stream().anyMatch(p->p.getId()==product.getId());
        };
    }
}
