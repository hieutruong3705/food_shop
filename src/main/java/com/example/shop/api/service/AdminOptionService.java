package com.example.shop.api.service;

import com.example.shop.domain.Brand;
import com.example.shop.domain.Category;
import com.example.shop.domain.Product;
import com.example.shop.repository.BrandRepository;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.ProductRepository;
import jakarta.persistence.criteria.Predicate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import static com.example.shop.api.service.AdminSupport.map;

/**
 * Searchable, paged lists of categories, brands and products for the admin pickers. Each row
 * carries what a picker needs to show it: a picture, and for groups how many products a shopper
 * can buy in them.
 */
@Service
@Transactional
public class AdminOptionService {
    private static final int PAGE_SIZE = 20;
    private static final int MAX_IDS = 500;

    private final ProductRepository products;
    private final CategoryRepository categories;
    private final BrandRepository brands;

    public AdminOptionService(ProductRepository products, CategoryRepository categories, BrandRepository brands) {
        this.products = products;
        this.categories = categories;
        this.brands = brands;
    }

    /**
     * @param categoryId  products: only this category's; brands: only the brands assigned to it
     * @param brandId     products: only this brand's
     * @param categoryIds brands: only those assigned to any of these categories
     * @param selected    only these ids, used to look up the names of what is already chosen
     */
    @Transactional(readOnly = true)
    public Page<Map<String, Object>> options(String type, int page, String search, Long categoryId, Long brandId,
            List<Long> categoryIds, List<Long> selected) {
        if (page < 0 || search.length() > 100 || selected.size() > MAX_IDS || categoryIds.size() > MAX_IDS) {
            throw new IllegalArgumentException("Bộ chọn không hợp lệ");
        }
        var paging = PageRequest.of(page, PAGE_SIZE, Sort.by("name").ascending().and(Sort.by("id")));
        String like = "%" + search.toLowerCase(Locale.ROOT) + "%";
        return switch (type) {
            case "categories" -> categoryRows(categories.findAll((root, q, cb) -> cb.and(
                    cb.like(cb.lower(root.get("name")), like),
                    selected.isEmpty() ? cb.conjunction() : root.get("id").in(selected)), paging));
            case "brands" -> brandRows(brands.findAll(brandSpec(like, categoryId, categoryIds, selected), paging));
            case "products" -> products.findAll(productSpec(like, categoryId, brandId, selected), paging).map(this::productRow);
            default -> throw new IllegalArgumentException("Bộ chọn không hợp lệ");
        };
    }

    private static Specification<Brand> brandSpec(String like, Long categoryId, List<Long> categoryIds, List<Long> selected) {
        return (root, q, cb) -> {
            var filters = new ArrayList<Predicate>();
            filters.add(cb.like(cb.lower(root.get("name")), like));
            if (!selected.isEmpty()) filters.add(root.get("id").in(selected));
            if (categoryId != null || !categoryIds.isEmpty()) {
                var sub = q.subquery(Long.class);
                var category = sub.from(Category.class);
                var brand = category.join("brands");
                sub.select(brand.get("id")).where(category.get("id").in(categoryId == null ? categoryIds : List.of(categoryId)));
                filters.add(root.get("id").in(sub));
            }
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }

    private static Specification<Product> productSpec(String like, Long categoryId, Long brandId, List<Long> selected) {
        return (root, q, cb) -> {
            var filters = new ArrayList<Predicate>();
            filters.add(cb.or(cb.like(cb.lower(root.get("name")), like), cb.like(cb.lower(root.get("sku")), like)));
            if (!selected.isEmpty()) filters.add(root.get("id").in(selected));
            if (categoryId != null) filters.add(cb.equal(root.get("category").get("id"), categoryId));
            if (brandId != null) filters.add(cb.equal(root.get("brand").get("id"), brandId));
            return cb.and(filters.toArray(Predicate[]::new));
        };
    }

    private Page<Map<String, Object>> categoryRows(Page<Category> page) {
        var counts = counts(products.sellableCountsByCategory(idsOrPlaceholder(page.map(Category::getId).getContent())));
        return page.map(c -> map("id", c.getId(), "name", c.getName(), "image", c.getImage(), "productCount", counts.getOrDefault(c.getId(), 0L)));
    }

    private Page<Map<String, Object>> brandRows(Page<Brand> page) {
        var counts = counts(products.sellableCountsByBrand(idsOrPlaceholder(page.map(Brand::getId).getContent())));
        return page.map(b -> map("id", b.getId(), "name", b.getName(), "image", b.getLogoUrl(), "productCount", counts.getOrDefault(b.getId(), 0L)));
    }

    private Map<String, Object> productRow(Product p) {
        return map("id", p.getId(), "name", p.getName(), "sku", p.getSku(), "price", p.getPrice(), "image", p.getImage(),
                "brand", p.getBrand() == null ? null : p.getBrand().getName(), "brandId", p.getBrand() == null ? null : p.getBrand().getId(),
                "category", p.getCategory() == null ? null : p.getCategory().getName(), "categoryId", p.getCategory() == null ? null : p.getCategory().getId(),
                "quantity", p.getQuantity(), "active", p.isActive());
    }

    /** An empty IN list is not valid SQL on every database; no product has this id, so it matches nothing. */
    private static List<Long> idsOrPlaceholder(List<Long> values) {
        return values.isEmpty() ? List.of(-1L) : values;
    }

    private static Map<Long, Long> counts(List<Object[]> rows) {
        var result = new HashMap<Long, Long>();
        for (Object[] row : rows) result.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
        return result;
    }
}
