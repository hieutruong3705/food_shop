package com.example.shop.api.controller;
import com.example.shop.api.service.CommerceService;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.dto.CommerceResponse.*;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/orders")
public class OrderApiController {
    private final CommerceService service;
    public OrderApiController(CommerceService service) { this.service=service; }
    @GetMapping public List<OrderView> list(Authentication a) { return service.list(a.getName()); }
    @PostMapping @ResponseStatus(org.springframework.http.HttpStatus.CREATED)
    public OrderView create(Authentication a,@RequestHeader("Idempotency-Key") String key,@Valid @RequestBody Checkout input) { return service.checkout(a.getName(),key,input); }
    @GetMapping("/{id}") public OrderView get(Authentication a,@PathVariable long id) { return service.detail(a.getName(),id,false); }
    @PatchMapping("/{id}/status") public OrderView cancel(Authentication a,@PathVariable long id,@Valid @RequestBody Status input) { return service.status(a.getName(),id,input.status(),false); }
}
