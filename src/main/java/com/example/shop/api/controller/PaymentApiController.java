package com.example.shop.api.controller;
import com.example.shop.api.dto.PaymentResponse;
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
    @GetMapping("/orders/{id}/payment-status") public PaymentResponse.StatusView state(Authentication a,@PathVariable long id){return payments.status(commerce.readOrder(a.getName(),id,false));}
    @GetMapping("/payments/vnpay/ipn") public Map<String,String> ipn(@RequestParam Map<String,String> params){return payments.ipn(params);}
    /** Signature-gated read of the payer's return URL. The session may be gone after the redirect, so this needs no login. */
    @GetMapping("/payments/vnpay/return") public PaymentResponse.ReturnView returned(@RequestParam Map<String,String> params){return payments.verifyReturn(params);}
    /** Admin reconciliation for orders whose IPN never arrived. The query runs outside the row lock that settling takes. */
    @PostMapping("/admin/orders/{id}/payment/reconcile") public Map<String,Object> reconcile(@PathVariable long id){return payments.settle(id,payments.query(id));}
}
