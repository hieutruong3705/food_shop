package com.example.shop.api.controller;
import com.example.shop.api.service.CommerceService;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.dto.CommerceResponse.*;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/cart")
public class CartApiController {
    private final CommerceService service;
    public CartApiController(CommerceService service) { this.service=service; }
    @GetMapping public CartView get(Authentication a) { return service.getCart(a.getName()); }
    @GetMapping("/quote") public Quote quote(Authentication a,@RequestParam(defaultValue="") String voucherCode,@RequestParam(required=false) java.util.List<Long> cartItemIds) { return service.quote(a.getName(),voucherCode,cartItemIds); }
    @PostMapping("/items") public CartView add(Authentication a,@Valid @RequestBody AddItem input) { return service.add(a.getName(),input); }
    @PatchMapping("/items/{id}") public CartView update(Authentication a,@PathVariable long id,@Valid @RequestBody Quantity input) { return service.update(a.getName(),id,input.quantity()); }
    @DeleteMapping("/items/{id}") public CartView delete(Authentication a,@PathVariable long id) { return service.update(a.getName(),id,null); }
}
