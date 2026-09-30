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
@RequestMapping("/api/v1/admin/vouchers")
public class VoucherAdminController {
    private final OfferAdminService service;
    private final AdminInputReader inputs;
    public VoucherAdminController(OfferAdminService service,AdminInputReader inputs) {
        this.service=service;
        this.inputs=inputs;
    }
    @GetMapping
    public Page<?> list(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,
            @RequestParam(defaultValue="") String search,@RequestParam Map<String,String> filters) {
        return service.listVouchers(page,size,search,filters);
    }
    @GetMapping("/{id}")
    public Object detail(@PathVariable long id) { return service.detailVoucher(id); }
    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable long id,Authentication a) { service.deleteVoucher(id); }
    @PostMapping
    public ResponseEntity<Map<String,Object>> create(Authentication a,@RequestBody JsonNode body) {
        VoucherInput input=inputs.readValidated(body,VoucherInput.class);
        Set<String> present=new HashSet<>();body.fieldNames().forEachRemaining(present::add);
        var saved=service.saveVoucher(null,input,present);
        return ResponseEntity.created(URI.create("/api/v1/admin/vouchers/"+saved.get("id"))).body(saved);
    }
    @PutMapping("/{id}")
    public Map<String,Object> update(Authentication a,@PathVariable long id,@RequestBody JsonNode body) {
        VoucherInput input=inputs.readValidated(body,VoucherInput.class);
        Set<String> present=new HashSet<>();body.fieldNames().forEachRemaining(present::add);
        return service.saveVoucher(id,input,present);
    }

}
