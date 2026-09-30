package com.example.shop.api.controller;
import com.example.shop.api.service.CatalogReadService;

import java.util.List;
import java.util.Map;

import com.example.shop.api.dto.catalog.ProductSummaryResponse;
import com.example.shop.domain.Banner;
import com.example.shop.domain.Category;
import com.example.shop.service.BannerService;
import com.example.shop.service.CategoryService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class CatalogApiController {
    private final CatalogReadService service;
    public CatalogApiController(CatalogReadService service) {
        this.service = service;
    }

    @GetMapping("/products")
    
    public org.springframework.data.domain.Page<ProductSummaryResponse> products(@RequestParam(defaultValue="") String search,
            @RequestParam(required=false) Long categoryId, @RequestParam(required=false) Long brandId,
            @RequestParam(required=false) Double minPrice, @RequestParam(required=false) Double maxPrice,
            @RequestParam(defaultValue="false") boolean inStock,
            @RequestParam(required=false) List<Long> brandIds,
            @RequestParam(required=false) Long promotionId,
            @RequestParam(required=false) Integer minWarranty, @RequestParam(required=false) Integer maxWarranty,
            @RequestParam org.springframework.util.MultiValueMap<String,String> parameters,
            @RequestParam(defaultValue="newest") String sort, @RequestParam(defaultValue="0") int page, @RequestParam(defaultValue="12") int size) {
        return service.products(search, categoryId, brandId, minPrice, maxPrice, inStock, brandIds, promotionId, minWarranty, maxWarranty, parameters, sort, page, size);
    }
    @GetMapping("/brands") public List<Map<String,Object>> brands() {
        return service.brands();
    }
    @GetMapping("/catalog/menu")
    
    public Map<String,Object> menu(@RequestParam long categoryId) {
        return service.menu(categoryId);
    }
    /** Facet counts describe the category before other filters; inactive products never contribute. */
    @GetMapping("/catalog/facets")
    
    public Map<String,Object> facets(@RequestParam(required=false) Long categoryId) {
        return service.facets(categoryId);
    }
    @GetMapping("/categories")
    public List<Map<String, Object>> categories() {
        return service.categories();
    }
    @GetMapping("/banners")
    public List<Map<String, Object>> banners() {
        return service.banners();
    }

}
