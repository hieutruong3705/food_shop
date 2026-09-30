package com.example.shop.api.service;

import com.example.shop.repository.ProductRepository;
import com.example.shop.repository.ProductVariantRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.Objects;

@Service
@Transactional(readOnly = true)
public class SkuService {
    private final ProductRepository products;
    private final ProductVariantRepository variants;

    public SkuService(ProductRepository products, ProductVariantRepository variants) {
        this.products = products;
        this.variants = variants;
    }

    public void checkProduct(String sku, long id) {
        check(sku, id, null);
    }

    public void checkVariant(String sku, Long id) {
        check(sku, null, id);
    }

    private void check(String sku, Long productId, Long variantId) {
        products.findAllBySkuIgnoreCase(sku).stream().filter(p -> !Objects.equals(p.getId(), productId))
                .findFirst().ifPresent(p -> { throw duplicate(sku, p.getName()); });
        variants.findAllBySkuIgnoreCase(sku).stream().filter(v -> !Objects.equals(v.getId(), variantId))
                .findFirst().ifPresent(v -> { throw duplicate(sku, v.getProduct().getName()); });
    }

    private IllegalArgumentException duplicate(String sku, String name) {
        return new IllegalArgumentException("SKU " + sku + " đã được dùng bởi sản phẩm " + name);
    }
}
