package com.example.shop.repository;
import com.example.shop.domain.ProductVariant;
public interface ProductVariantRepository extends org.springframework.data.jpa.repository.JpaRepository<ProductVariant,Long> {
    boolean existsBySku(String sku);
    java.util.List<ProductVariant> findAllBySkuIgnoreCase(String sku);
}
