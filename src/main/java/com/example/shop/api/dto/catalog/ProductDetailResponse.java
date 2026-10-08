package com.example.shop.api.dto.catalog;
import com.example.shop.api.service.PromotionPricing.Discounts;
import com.example.shop.domain.Product;
import java.util.List;
import java.util.Map;
public record ProductDetailResponse(ProductSummaryResponse product, String detailDescription, List<String> images, List<Map<String,String>> specifications, String sku, String slug, Integer warrantyMonths, Long categoryId, List<VariantResponse> variants, boolean hasVariants) {
 public static ProductDetailResponse from(Product p, Discounts discounts) { return from(p, discounts, java.util.Map.of()); }
 public static ProductDetailResponse from(Product p, Discounts discounts, java.util.Map<Long, com.example.shop.api.service.ProductRatings.Summary> ratings) { return new ProductDetailResponse(ProductSummaryResponse.from(p, discounts, ratings), p.getDetailDesc(), p.getImages().stream().map(i->i.getImageUrl()).toList(), p.getSpecs().stream().map(s->Map.of("name",s.getSpecName(),"value",s.getSpecValue())).toList(),p.getSku(),p.getSlug(),p.getWarrantyMonths(),p.getCategory()==null?null:p.getCategory().getId(),p.getVariants().stream().filter(v->v.isActive()).map(v->VariantResponse.from(v, discounts)).toList(),p.hasVariants()); }
}
