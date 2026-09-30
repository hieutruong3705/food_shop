package com.example.shop;

import com.example.shop.config.PaymentExpiryJob;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ActiveProfiles;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;
import static org.assertj.core.api.Assertions.assertThat;

/** VNPay is switched off here: unpaid VNPAY orders must still expire and release their stock. */
@SpringBootTest(properties = "shop.vnpay.enabled=false")
@ActiveProfiles("test")
class PaymentExpiryJobTests {
    @Autowired ApplicationContext context;
    @Autowired UserRepository users;
    @Autowired RoleRepository roles;
    @Autowired ProductRepository products;
    @Autowired OrderRepository orders;

    @Test
    void expiryJobExistsAndReleasesStockWhileVnpayIsDisabled() {
        assertThat(context.getBeansOfType(PaymentExpiryJob.class)).hasSize(1);

        var role = roles.findByName("USER");
        if (role == null) {
            role = new Role();
            role.setName("USER");
            role = roles.save(role);
        }
        var buyer = new User();
        buyer.setEmail(UUID.randomUUID() + "@test.local");
        buyer.setRole(role);
        buyer.setEnabled(true);
        buyer = users.save(buyer);

        var product = new Product();
        product.setName("Expiry job product");
        product.setPrice(1500000);
        product.setQuantity(1);
        product.setSold(1);
        product = products.save(product);

        var detail = new OrderDetail();
        detail.setProduct(product);
        detail.setProductName(product.getName());
        detail.setQuantity(1);
        detail.setPrice(1500000);
        var order = new Order();
        order.setUser(buyer);
        order.setStatus(OrderStatus.PENDING);
        order.setPaymentMethod("VNPAY");
        order.setPaymentStatus(PaymentStatus.UNPAID);
        order.setTotalPrice(1500000);
        order.setOrderDetails(List.of(detail));
        detail.setOrder(order);
        order = orders.saveAndFlush(order);
        // createdAt is assigned on insert; move it past the 20-minute payment window afterwards.
        order.setCreatedAt(Date.from(Instant.now().minus(Duration.ofMinutes(25))));
        orders.saveAndFlush(order);

        context.getBean(PaymentExpiryJob.class).expire();

        var expired = orders.findById(order.getId()).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(expired.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
        var restored = products.findById(product.getId()).orElseThrow();
        assertThat(restored.getQuantity()).isEqualTo(2);
        assertThat(restored.getSold()).isZero();
    }
}
