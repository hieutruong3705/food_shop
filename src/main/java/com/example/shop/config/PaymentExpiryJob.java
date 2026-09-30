package com.example.shop.config;
import com.example.shop.repository.OrderRepository;
import com.example.shop.api.service.SandboxPaymentService;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.*;
/** Always runs: unpaid VNPAY orders must release stock even after VNPay has been switched off. */
@Configuration @EnableScheduling
public class PaymentExpiryJob {
    private final OrderRepository orders;private final SandboxPaymentService payments;private final java.time.Clock clock;
    public PaymentExpiryJob(OrderRepository o,SandboxPaymentService p,java.time.Clock clock){orders=o;payments=p;this.clock=clock;}
    @Scheduled(fixedDelay=60000,initialDelay=60000) public void expire(){for(var order:orders.findByPaymentMethodAndStatusAndPaymentStatusAndCreatedAtBefore("VNPAY","PENDING","UNPAID",java.util.Date.from(clock.instant().minusSeconds(1200))))payments.expire(order.getId());}
}
