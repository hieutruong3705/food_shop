package com.example.shop.domain;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The one definition of "this offer applies to that product". Everything that needs the answer
 * uses it: the sale price, the voucher discount at checkout, the public offers list, the
 * {@code promotionId} product filter and the admin preview. It exists in two forms that must
 * agree, an in-memory test ({@link Scope#matches}) and a database predicate
 * ({@link Scope#predicate}); {@code OfferScopeTests} runs both over the same data.
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

        /** A scope that lists nothing selects nothing; the admin API refuses to save one. */
        public boolean isEmpty() {
            return switch (type) {
                case ALL -> false;
                case CATALOG -> categoryIds.isEmpty() && brandIds.isEmpty();
                case PRODUCT -> productIds.isEmpty();
            };
        }

        public boolean matches(Product product) {
            return matches(product.getCategory() == null ? null : product.getCategory().getId(),
                    product.getBrand() == null ? null : product.getBrand().getId(), product.getId());
        }

        public boolean matches(Long categoryId, Long brandId, long productId) {
            if (isEmpty()) return false;
            return switch (type) {
                case ALL -> true;
                case CATALOG -> (categoryIds.isEmpty() || (categoryId != null && categoryIds.contains(categoryId)))
                        && (brandIds.isEmpty() || (brandId != null && brandIds.contains(brandId)));
                case PRODUCT -> productIds.contains(productId);
            };
        }

        /** The same test as {@link #matches(Long, Long, long)}, evaluated by the database. */
        public Predicate predicate(Root<Product> root, CriteriaBuilder cb) {
            if (isEmpty()) return cb.disjunction();
            return switch (type) {
                case ALL -> cb.conjunction();
                case CATALOG -> cb.and(
                        categoryIds.isEmpty() ? cb.conjunction() : root.get("category").get("id").in(categoryIds),
                        brandIds.isEmpty() ? cb.conjunction() : root.get("brand").get("id").in(brandIds));
                case PRODUCT -> root.get("id").in(productIds);
            };
        }
    }
}
