package com.example.shop.api.dto.catalog;
import com.example.shop.domain.Product;
import java.util.List;
import java.util.Map;
public record ProductDetailResponse(ProductSummaryResponse product, String detailDescription, List<String> images, List<Map<String,String>> specifications, String sku, String slug, Integer warrantyMonths, Long categoryId, List<VariantResponse> variants, boolean hasVariants) {
 public static ProductDetailResponse from(Product p) { return new ProductDetailResponse(ProductSummaryResponse.from(p), p.getDetailDesc(), p.getImages().stream().map(i->i.getImageUrl()).toList(), p.getSpecs().stream().map(s->Map.of("name",s.getSpecName(),"value",s.getSpecValue())).toList(),p.getSku(),p.getSlug(),p.getWarrantyMonths(),p.getCategory()==null?null:p.getCategory().getId(),p.getVariants().stream().filter(v->v.isActive()).map(VariantResponse::from).toList(),p.hasVariants()); }
}
