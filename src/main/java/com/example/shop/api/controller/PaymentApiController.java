package com.example.shop.api.controller;
import com.example.shop.api.service.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;
import java.util.Map;
@RestController @RequestMapping("/api/v1")
public class PaymentApiController {
    private final SandboxPaymentService payments;private final CommerceService commerce;
    public PaymentApiController(SandboxPaymentService p,CommerceService c){payments=p;commerce=c;}
    @GetMapping("/payments/options") public Map<String,Object> options(){return Map.of("cod",true,"vnpay",payments.available(),"sandbox",true);}
    @PostMapping("/orders/{id}/payment") @Transactional public Map<String,String> create(Authentication a,@PathVariable long id,jakarta.servlet.http.HttpServletRequest request){return Map.of("url",payments.paymentUrl(commerce.requireOrder(a.getName(),id,false),request.getRemoteAddr()));}
    @GetMapping("/payments/vnpay/ipn") public Map<String,String> ipn(@RequestParam Map<String,String> params){return payments.ipn(params);}
}
