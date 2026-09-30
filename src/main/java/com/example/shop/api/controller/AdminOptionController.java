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
@RequestMapping("/api/v1/admin")
public class AdminOptionController {
    private final AdminOptionService options;
    private final OfferScopeService scopes;
    private final AdminInputReader inputs;
    public AdminOptionController(AdminOptionService options,OfferScopeService scopes,AdminInputReader inputs) {
        this.options=options; this.scopes=scopes; this.inputs=inputs;
    }
    @GetMapping("/options/{resource}")
    public Page<Map<String,Object>> options(@PathVariable String resource,@RequestParam(defaultValue="0") int page,
            @RequestParam(defaultValue="") String search,@RequestParam(required=false) Long categoryId,
            @RequestParam(required=false) Long brandId,
            @RequestParam(defaultValue="") List<Long> categoryIds,@RequestParam(defaultValue="") List<Long> ids) {
        return options.options(resource,page,search,categoryId,brandId,categoryIds,ids);
    }
    @PostMapping("/offers/preview")
    public Map<String,Object> preview(@RequestParam String kind,@RequestBody JsonNode body) {
        if(!Set.of("voucher","promotion").contains(kind))throw new IllegalArgumentException("Loại ưu đãi không hợp lệ");
        try { return scopes.preview(inputs.scope(body),kind.equals("voucher")); }
        catch(IllegalArgumentException e) { return Map.of("valid",false,"message",e.getMessage(),"total",0,"inStock",0,"sample",List.of()); }
    }
}
