package com.example.shop.config;
import com.example.shop.repository.OrderRepository;
import com.example.shop.api.service.SandboxPaymentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
@Configuration @EnableScheduling @ConditionalOnProperty(name="shop.vnpay.enabled",havingValue="true")
public class PaymentExpiryJob {
    private final OrderRepository orders;private final SandboxPaymentService payments;
    public PaymentExpiryJob(OrderRepository o,SandboxPaymentService p){orders=o;payments=p;}
    @Scheduled(fixedDelay=60000,initialDelay=60000) public void expire(){for(var order:orders.findByPaymentMethodAndStatusAndPaymentStatusAndCreatedAtBefore("VNPAY","PENDING","UNPAID",java.util.Date.from(java.time.Instant.now().minusSeconds(1200))))payments.expire(order.getId());}
}
