package com.example.shop.api.controller;
import com.example.shop.api.dto.admin.*;
import com.example.shop.api.service.*;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.*;

@RestController
@RequestMapping("/api/v1/admin/promotions")
public class PromotionAdminController {
    private final OfferAdminService service;
    public PromotionAdminController(OfferAdminService service) {
        this.service=service;
    }
    @GetMapping
    public Page<?> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="") String search,@RequestParam Map<String,String> filters) {
        return service.listPromotions(page,size,search,filters);
    }
    @GetMapping("/{id}")
    public Object detail(@PathVariable long id) { return service.detailPromotion(id); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id,Authentication a) { service.deletePromotion(id); }
    @PostMapping
    public ResponseEntity<Map<String,Object>> create(Authentication a,@Valid @RequestBody PromotionInput input) {
        var saved=service.savePromotion(null,input);
        return ResponseEntity.created(URI.create("/api/v1/admin/promotions/"+saved.get("id"))).body(saved);
    }
    @PutMapping("/{id}")
    public Map<String,Object> update(Authentication a,@PathVariable long id,@Valid @RequestBody PromotionInput input) {
        return service.savePromotion(id,input);
    }

}
