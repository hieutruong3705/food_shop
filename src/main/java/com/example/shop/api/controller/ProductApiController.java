package com.example.shop.api.controller;
import com.example.shop.api.service.ProductReadService;
import com.example.shop.api.dto.catalog.ProductDetailResponse;
import com.example.shop.api.exception.ResourceNotFoundException;
import com.example.shop.api.service.PromotionPricing;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/products")
public class ProductApiController {
    private final ProductReadService service;
    public ProductApiController(ProductReadService service) {
        this.service = service;
    }

 @GetMapping("/{id}") public ProductDetailResponse detail(@PathVariable long id) {
        return service.detail(id);
    }
}
