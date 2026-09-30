package com.example.shop.api.controller;
import com.example.shop.api.dto.catalog.ProductDetailResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.service.ProductService;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/products")
public class ProductApiController {
 private final ProductService productService;
 public ProductApiController(ProductService productService) { this.productService=productService; }
 @GetMapping("/{id}") public ProductDetailResponse detail(@PathVariable long id) { var p=productService.fetchProductById(id).orElseThrow(()->new ResourceNotFoundException("Sản phẩm",id)); if(!p.isActive() || !p.isTechnologyCatalog()) throw new ResourceNotFoundException("Sản phẩm",id); return ProductDetailResponse.from(p); }
}
