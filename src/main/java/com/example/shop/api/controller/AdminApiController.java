package com.example.shop.api.controller;
import com.example.shop.api.service.AdminOrderService;
import com.example.shop.api.service.*;
import com.example.shop.api.dto.CommerceResponse;
import com.example.shop.api.dto.CommerceRequests.Status;
import com.example.shop.repository.*;
import com.example.shop.service.InvoiceService;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.data.domain.*;
import java.util.*;

@RestController @RequestMapping("/api/v1/admin") 
public class AdminApiController {
    private final AdminOrderService service;
    public AdminApiController(AdminOrderService service) {
        this.service = service;
    }

    @GetMapping("/dashboard") public Map<String,Object> dashboard(@RequestParam(required=false) java.time.LocalDate startDate,@RequestParam(required=false) java.time.LocalDate endDate){
        return service.dashboard(startDate, endDate);
    }
    @GetMapping("/orders") public Page<CommerceResponse.OrderView> orders(@RequestParam(defaultValue="0") int page,@RequestParam(defaultValue="20") int size,@RequestParam(defaultValue="") String search,@RequestParam(defaultValue="") String status,@RequestParam(defaultValue="false") boolean archived,@RequestParam(defaultValue="false") boolean refundRequired){
        return service.orders(page, size, search, status, archived, refundRequired);
    }
    @GetMapping("/orders/{id}") public CommerceResponse.OrderView order(Authentication a,@PathVariable long id){
        return service.order(a, id);
    }
    @DeleteMapping("/orders/{id}") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT) public void archive(Authentication a,@PathVariable long id){
        service.archive(a, id);
    }
    @PostMapping("/orders/{id}/restore") public CommerceResponse.OrderView restore(Authentication a,@PathVariable long id){
        return service.restore(a, id);
    }
    @PatchMapping("/orders/{id}/status") public CommerceResponse.OrderView status(Authentication a,@PathVariable long id,@Valid @RequestBody Status s){
        return service.status(a, id, s);
    }
    @GetMapping("/orders/{id}/invoice") public void invoice(Authentication a,@PathVariable long id,jakarta.servlet.http.HttpServletResponse response){
        service.invoice(a, id, response);
    }
    @GetMapping(value="/reports/orders",produces="text/csv;charset=UTF-8") public org.springframework.http.ResponseEntity<String> report(){
        return service.report();
    }
}
