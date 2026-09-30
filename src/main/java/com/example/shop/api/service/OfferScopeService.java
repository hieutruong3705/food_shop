package com.example.shop.api.service;

import com.example.shop.domain.Brand;
import com.example.shop.domain.Category;
import com.example.shop.domain.OfferScopeRule.Scope;
import com.example.shop.domain.Product;
import com.example.shop.domain.ScopeType;
import com.example.shop.domain.ScopedOffer;
import com.example.shop.repository.BrandRepository;
import com.example.shop.repository.CategoryRepository;
import com.example.shop.repository.OfferScopeSpecification;
import com.example.shop.repository.ProductRepository;
import com.example.shop.api.dto.admin.ScopeInput;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Reads, validates and previews the "applies to" part of a voucher or promotion payload.
 *
 * <p>Payload: {@code scopeType} (ALL, CATEGORY, BRAND or PRODUCT) plus {@code categoryIds},
 * {@code brandIds} and {@code productIds}. CATEGORY takes whole categories and/or single products,
 * BRAND takes whole brands and/or single products, PRODUCT takes products; lists that do not belong
 * to the chosen type are ignored, and so are products a chosen category or brand already covers. A
 * missing {@code scopeType} is inferred from whichever list is filled (vouchers may also send
 * {@code all}), which keeps older payloads working.
 */
@Service
@Transactional
public class OfferScopeService {
    private static final int MAX_ITEMS = 500;
    private static final int SAMPLE_SIZE = 8;

    private final CategoryRepository categories;
    private final BrandRepository brands;
    private final ProductRepository products;

    public OfferScopeService(CategoryRepository categories, BrandRepository brands, ProductRepository products) {
        this.categories = categories;
        this.brands = brands;
        this.products = products;
    }

    private record Resolved(ScopeType type, List<Category> categories, List<Brand> brands, List<Product> products) {
        Scope scope() {
            return new Scope(type, ids(categories.stream().map(Category::getId)), ids(brands.stream().map(Brand::getId)),
                    ids(products.stream().map(Product::getId)));
        }

        private static Set<Long> ids(java.util.stream.Stream<Long> stream) {
            return stream.collect(Collectors.toUnmodifiableSet());
        }
    }

    /** Validates the scope in the payload and stores it on the offer, replacing its previous scope. */
    public void apply(ScopedOffer offer, ScopeInput payload, boolean voucher) {
        Resolved scope = resolve(payload, voucher);
        offer.setScopeType(scope.type());
        offer.setCategories(new ArrayList<>(scope.categories()));
        offer.setBrands(new ArrayList<>(scope.brands()));
        offer.setProducts(new ArrayList<>(scope.products()));
    }

    /**
     * What the scope would cover right now, for the admin form: how many sellable products, and a
     * few of them with their pictures. An invalid or empty selection is reported, not thrown, so the
     * form can show the reason while the admin is still choosing.
     */
    @Transactional(readOnly = true)
    public Map<String, Object> preview(ScopeInput payload, boolean voucher) {
        var result = new LinkedHashMap<String, Object>();
        Resolved resolved;
        try {
            resolved = resolve(payload, voucher);
        } catch (IllegalArgumentException e) {
            result.put("valid", false);
            result.put("message", e.getMessage());
            result.put("total", 0L);
            result.put("inStock", 0L);
            result.put("sample", List.of());
            return result;
        }
        Scope scope = resolved.scope();
        var visible = OfferScopeSpecification.visible(scope);
        long total = products.count(visible);
        long inStock = products.count(visible.and((root, q, cb) -> cb.gt(root.get("quantity"), 0)));
        var sample = new ArrayList<Map<String, Object>>();
        for (Product p : products.findAll(visible, PageRequest.of(0, SAMPLE_SIZE, Sort.by("id").descending())).getContent()) {
            var row = new LinkedHashMap<String, Object>();
            row.put("id", p.getId());
            row.put("name", p.getName());
            row.put("sku", p.getSku());
            row.put("price", p.getPrice());
            row.put("image", p.getImage());
            row.put("brand", p.getBrand() == null ? null : p.getBrand().getName());
            row.put("category", p.getCategory() == null ? null : p.getCategory().getName());
            row.put("quantity", p.getQuantity());
            sample.add(row);
        }
        result.put("valid", true);
        result.put("scopeType", resolved.type());
        result.put("total", total);
        result.put("inStock", inStock);
        result.put("sample", sample);
        return result;
    }

    private Resolved resolve(ScopeInput n, boolean voucher) {
        String declared = n.scopeType() == null ? "" : n.scopeType().trim();
        // CATALOG was the short-lived combined type; its payloads are read like ones without a type.
        if (declared.equalsIgnoreCase("CATALOG")) declared = "";
        List<Long> categoryIds = ids(n.categoryIds());
        List<Long> brandIds = ids(n.brandIds());
        List<Long> productIds = ids(n.productIds());
        ScopeType type;
        if (declared.isEmpty()) {
            if (voucher && Boolean.TRUE.equals(n.all())) type = ScopeType.ALL;
            else if (!categoryIds.isEmpty() && !brandIds.isEmpty()) throw new IllegalArgumentException("Chọn theo danh mục hoặc theo thương hiệu, không chọn cả hai");
            else if (!categoryIds.isEmpty()) type = ScopeType.CATEGORY;
            else if (!brandIds.isEmpty()) type = ScopeType.BRAND;
            else if (!productIds.isEmpty()) type = ScopeType.PRODUCT;
            else throw new IllegalArgumentException("Chọn ít nhất một mục cho phạm vi áp dụng");
        } else {
            type = ScopeType.parse(declared);
        }
        switch (type) {
            case ALL -> {
                return new Resolved(type, List.of(), List.of(), List.of());
            }
            case CATEGORY -> {
                brandIds = List.of();
                if (categoryIds.isEmpty() && productIds.isEmpty()) throw new IllegalArgumentException("Chọn ít nhất một danh mục hoặc một sản phẩm");
            }
            case BRAND -> {
                categoryIds = List.of();
                if (brandIds.isEmpty() && productIds.isEmpty()) throw new IllegalArgumentException("Chọn ít nhất một thương hiệu hoặc một sản phẩm");
            }
            case PRODUCT -> {
                categoryIds = List.of();
                brandIds = List.of();
                if (productIds.isEmpty()) throw new IllegalArgumentException("Chọn ít nhất một sản phẩm");
            }
        }
        var cs = categories.findAllById(categoryIds);
        var bs = brands.findAllById(brandIds);
        var ps = products.findAllById(productIds);
        if (cs.size() != new HashSet<>(categoryIds).size() || bs.size() != new HashSet<>(brandIds).size()
                || ps.size() != new HashSet<>(productIds).size()) {
            throw new IllegalArgumentException("Mục áp dụng không tồn tại");
        }
        // A product already covered by a whole category or brand of the same offer adds nothing.
        Set<Long> coveredCategories = cs.stream().map(Category::getId).collect(Collectors.toSet());
        Set<Long> coveredBrands = bs.stream().map(Brand::getId).collect(Collectors.toSet());
        ps = ps.stream().filter(p -> !(p.getCategory() != null && coveredCategories.contains(p.getCategory().getId()))
                && !(p.getBrand() != null && coveredBrands.contains(p.getBrand().getId()))).toList();
        return new Resolved(type, cs, bs, ps);
    }

    private static List<Long> ids(List<Long> values) {
        if (values == null) return List.of();
        if (values.size() > MAX_ITEMS) throw new IllegalArgumentException("Mỗi danh sách tối đa " + MAX_ITEMS + " mục");
        if (values.stream().anyMatch(value -> value == null || value < 1)) throw new IllegalArgumentException("ID không hợp lệ");
        return values;
    }
}
