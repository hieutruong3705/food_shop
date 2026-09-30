package com.example.shop.repository;
import com.example.shop.domain.*;
import org.springframework.data.jpa.domain.Specification;

/** Products a shopper can buy that fall inside an offer's scope, judged by {@link OfferScopeRule}. */
public final class OfferScopeSpecification {
    private OfferScopeSpecification(){}
    public static Specification<Product> visible(ScopedOffer offer){return visible(offer.scope());}
    public static Specification<Product> visible(OfferScopeRule.Scope scope){
        return (root,q,cb)->cb.and(cb.isTrue(root.get("active")),cb.isTrue(root.get("technologyCatalog")),scope.predicate(root,cb));
    }
}
