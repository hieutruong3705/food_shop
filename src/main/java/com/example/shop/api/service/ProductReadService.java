package com.example.shop.api.service;
import org.springframework.transaction.annotation.Transactional;
import com.example.shop.api.dto.catalog.ProductDetailResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.api.service.PromotionPricing;
import com.example.shop.repository.ProductRepository;
import org.springframework.web.bind.annotation.*;
@org.springframework.stereotype.Service 
@Transactional(readOnly=true)
public class ProductReadService {
 private final ProductRepository products; private final PromotionPricing pricing;
 public ProductReadService(ProductRepository products, PromotionPricing pricing) { this.products=products; this.pricing=pricing; }
  public ProductDetailResponse detail(long id) { var p=products.findById(id).orElseThrow(()->new ResourceNotFoundException("Sản phẩm",id)); if(!p.isActive() || !p.isTechnologyCatalog()) throw new ResourceNotFoundException("Sản phẩm",id); return ProductDetailResponse.from(p, pricing.current()); }
}
