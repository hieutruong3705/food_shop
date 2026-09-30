package com.example.shop.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The one definition of "this offer applies to that product". Everything that needs the answer
 * uses it: the sale price, the voucher discount at checkout, the public offers list, the
 * {@code promotionId} product filter and the admin preview. It exists in two forms that must
 * agree, an in-memory test ({@link Scope#matches}) and a database predicate
 * ({@link Scope#predicate}); {@code OfferScopeTests} runs both over the same data.
 *
 * <p>A scope is a union, never an intersection: a product matches when it is in one of the listed
 * categories (CATEGORY scopes), in one of the listed brands (BRAND scopes) or is one of the listed
 * products (all scopes but ALL). Lists that do not belong to the scope type are ignored.
 */
public final class OfferScopeRule {
    private OfferScopeRule() {}

    public record Scope(ScopeType type, Set<Long> categoryIds, Set<Long> brandIds, Set<Long> productIds) {
        public static final Scope ALL = new Scope(ScopeType.ALL, Set.of(), Set.of(), Set.of());

        public static Scope of(ScopedOffer offer) {
            return new Scope(offer.getScopeType(), ids(offer.getCategories()), ids(offer.getBrands()), ids(offer.getProducts()));
        }

        private static Set<Long> ids(List<? extends Object> items) {
            var result = new HashSet<Long>();
            for (Object item : items) {
                if (item instanceof Category c) result.add(c.getId());
                else if (item instanceof Brand b) result.add(b.getId());
                else if (item instanceof Product p) result.add(p.getId());
            }
            return Set.copyOf(result);
        }

        private Set<Long> wholeCategories() {
            return type == ScopeType.CATEGORY ? categoryIds : Set.of();
        }

        private Set<Long> wholeBrands() {
            return type == ScopeType.BRAND ? brandIds : Set.of();
        }

        private Set<Long> chosenProducts() {
            return type == ScopeType.ALL ? Set.of() : productIds;
        }

        /** A scope that lists nothing selects nothing; the admin API refuses to save one. */
        public boolean isEmpty() {
            return type != ScopeType.ALL && wholeCategories().isEmpty() && wholeBrands().isEmpty() && chosenProducts().isEmpty();
        }

        public boolean matches(Product product) {
            return matches(product.getCategory() == null ? null : product.getCategory().getId(),
                    product.getBrand() == null ? null : product.getBrand().getId(), product.getId());
        }

        public boolean matches(Long categoryId, Long brandId, long productId) {
            if (type == ScopeType.ALL) return true;
            return (categoryId != null && wholeCategories().contains(categoryId))
                    || (brandId != null && wholeBrands().contains(brandId))
                    || chosenProducts().contains(productId);
        }

        /** The same test as {@link #matches(Long, Long, long)}, evaluated by the database. */
        public Predicate predicate(Root<Product> root, CriteriaBuilder cb) {
            if (type == ScopeType.ALL) return cb.conjunction();
            if (isEmpty()) return cb.disjunction();
            var any = new ArrayList<Predicate>();
            if (!wholeCategories().isEmpty()) any.add(root.get("category").get("id").in(wholeCategories()));
            if (!wholeBrands().isEmpty()) any.add(root.get("brand").get("id").in(wholeBrands()));
            if (!chosenProducts().isEmpty()) any.add(root.get("id").in(chosenProducts()));
            return cb.or(any.toArray(Predicate[]::new));
        }
    }
}
