package com.example.shop.api.dto.catalog;
import com.example.shop.domain.ProductVariant;
import java.util.Map;
public record VariantResponse(Long id,String sku,Map<String,String> options,String label,double price,double discountedPrice,long quantity,boolean active,String imageUrl) {
    public static VariantResponse from(ProductVariant v){return new VariantResponse(v.getId(),v.getSku(),Map.copyOf(v.getOptions()),v.getLabel(),v.getPrice(),v.getProduct().discountedPrice(v.getPrice()),v.getQuantity(),v.isActive(),v.getImageUrl());}
}
