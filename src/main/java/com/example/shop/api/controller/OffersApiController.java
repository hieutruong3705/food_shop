package com.example.shop.api.controller;
import com.example.shop.api.service.OfferReadService;

import com.example.shop.repository.*;
import com.example.shop.domain.*;
import com.example.shop.api.service.AdminSupport;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.time.Clock;
import java.util.*;

@RestController @RequestMapping("/api/v1") 
public class OffersApiController {
    private final OfferReadService service;
    public OffersApiController(OfferReadService service) {
        this.service = service;
    }

    /** One readable sentence for the storefront, e.g. "Danh mục Laptop · Thương hiệu Dell, HP". */

    @GetMapping("/offers") public Map<String,Object> offers(){
        return service.offers();
    }
}
