package com.example.shop.api.service;

import com.example.shop.domain.OfferScopeRule.Scope;
import com.example.shop.domain.Product;
import com.example.shop.domain.Promotion;
import com.example.shop.repository.PromotionRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import java.time.Clock;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Sale prices. A promotion is running when it is active and {@code start < now < end} by the shared
 * {@link Clock}; the highest running rate among those whose scope covers the product wins.
 *
 * <p>The running promotions are read once per HTTP request (a few id-only queries, none at all when
 * nothing is running) and reused, so a product list costs the same however many products it has.
 */
@Service
public class PromotionPricing {
    private static final String REQUEST_KEY = PromotionPricing.class.getName() + ".discounts";

    private final PromotionRepository promotions;
    private final Clock clock;

    public PromotionPricing(PromotionRepository promotions, Clock clock) {
        this.promotions = promotions;
        this.clock = clock;
    }

    /** Sale prices as of now. Cheap to call repeatedly within a request. */
    @Transactional(readOnly = true)
    public Discounts current() {
        RequestAttributes request = RequestContextHolder.getRequestAttributes();
        if (request != null && request.getAttribute(REQUEST_KEY, RequestAttributes.SCOPE_REQUEST) instanceof Discounts cached) {
            return cached;
        }
        Discounts loaded = snapshot();
        if (request != null) request.setAttribute(REQUEST_KEY, loaded, RequestAttributes.SCOPE_REQUEST);
        return loaded;
    }

    @Transactional(readOnly = true)
    public Discounts snapshot() {
        List<Promotion> running = promotions.findRunning(Date.from(clock.instant()));
        if (running.isEmpty()) return Discounts.NONE;
        List<Long> ids = running.stream().map(Promotion::getId).toList();
        Map<Long, Set<Long>> categories = group(promotions.categoryLinks(ids));
        Map<Long, Set<Long>> brands = group(promotions.brandLinks(ids));
        Map<Long, Set<Long>> products = group(promotions.productLinks(ids));
        List<Entry> entries = new ArrayList<>();
        for (Promotion p : running) {
            Set<Long> none = Set.of();
            entries.add(new Entry(p.getId(), p.getName(), p.getEndDate(), Math.min(100, p.getDiscountRate()), new Scope(p.getScopeType(),
                    categories.getOrDefault(p.getId(), none), brands.getOrDefault(p.getId(), none), products.getOrDefault(p.getId(), none))));
        }
        return new Discounts(List.copyOf(entries));
    }

    private static Map<Long, Set<Long>> group(List<Object[]> links) {
        var result = new HashMap<Long, Set<Long>>();
        for (Object[] link : links) {
            result.computeIfAbsent(((Number) link[0]).longValue(), k -> new HashSet<>()).add(((Number) link[1]).longValue());
        }
        return result;
    }

    private record Entry(long promotionId, String name, Date endDate, double rate, Scope scope) {}

    /** The running promotion that gives a product its price: shown as the sale tag. */
    public record Sale(long promotionId, String name, double rate, Date endDate) {}

    public static final class Discounts {
        /** No promotion is running. */
        public static final Discounts NONE = new Discounts(List.of());

        private final List<Entry> entries;

        private Discounts(List<Entry> entries) {
            this.entries = entries;
        }

        /** The running promotion with the highest rate among those covering the product, or null. */
        public Sale sale(Product product) {
            Entry best = null;
            for (Entry entry : entries) {
                if ((best == null || entry.rate() > best.rate()) && entry.rate() > 0 && entry.scope().matches(product)) best = entry;
            }
            return best == null ? null : new Sale(best.promotionId(), best.name(), best.rate(), best.endDate());
        }

        /** Highest running discount rate (percent) that covers the product, or 0. */
        public double rate(Product product) {
            Sale sale = sale(product);
            return sale == null ? 0 : sale.rate();
        }

        /** The price a shopper pays for a product (or one of its variants) listed at {@code listPrice}. */
        public double price(Product product, double listPrice) {
            double rate = rate(product);
            return rate > 0 ? Math.round(listPrice * (1 - rate / 100)) : listPrice;
        }
    }
}
