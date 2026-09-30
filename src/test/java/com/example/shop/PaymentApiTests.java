package com.example.shop;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.api.service.*;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import static org.assertj.core.api.Assertions.*;
@SpringBootTest(properties={"shop.vnpay.enabled=true","shop.vnpay.tmn-code=TESTONLY","shop.vnpay.secret=local-test-signing-key"}) @ActiveProfiles("test")
class PaymentApiTests {
    @Autowired ProductVariantRepository variants;
    @Test void failedVariantPaymentRestoresSelectedSkuOnly()throws Exception{
        commerce.status(buyer.getEmail(),orderId,"CANCELLED",false);
        var v=new ProductVariant();v.setProduct(product);v.setSku(UUID.randomUUID().toString());v.setPrice(2000000);v.setQuantity(2);v.getOptions().put("Màu","Đen");v=variants.save(v);
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1,v.getId()));
        orderId=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),new Checkout("Test","Test address","0909876543",null,"VNPAY")).id();
        var signed=callback("24",200000000);assertThat(payments.ipn(signed).get("RspCode")).isEqualTo("00");assertThat(payments.ipn(signed).get("RspCode")).isEqualTo("02");
        assertThat(variants.findById(v.getId()).orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(2);
    }
    @Autowired CommerceService commerce;@Autowired SandboxPaymentService payments;@Autowired UserRepository users;@Autowired RoleRepository roles;@Autowired ProductRepository products;@Autowired OrderRepository orders;
    User buyer;Product product;long orderId;
    @BeforeEach void setup(){var role=roles.findByName("USER");if(role==null){role=new Role();role.setName("USER");role=roles.save(role);}buyer=new User();buyer.setEmail(UUID.randomUUID()+"@test.local");buyer.setRole(role);buyer.setEnabled(true);buyer=users.save(buyer);product=new Product();product.setName("Sandbox test product");product.setPrice(2000000);product.setQuantity(2);product=products.save(product);commerce.add(buyer.getEmail(),new AddItem(product.getId(),1));orderId=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),new Checkout("Test","Test address","0909876543",null,"VNPAY")).id();}
    Map<String,String> callback(String code,long amount)throws Exception {return callback(code,amount,Map.of());}
    Map<String,String> callback(String code,long amount,Map<String,String> extra)throws Exception {var fields=new TreeMap<String,String>(extra);fields.put("vnp_TmnCode","TESTONLY");fields.put("vnp_TxnRef",Long.toString(orderId));fields.put("vnp_Amount",Long.toString(amount));fields.put("vnp_ResponseCode",code);fields.put("vnp_TransactionStatus",code);var mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec("local-test-signing-key".getBytes(StandardCharsets.UTF_8),"HmacSHA512"));fields.put("vnp_SecureHash",HexFormat.of().formatHex(mac.doFinal(SandboxPaymentService.canonical(fields).getBytes(StandardCharsets.UTF_8))));return fields;}
    @Test void signatureAmountAndReplayAreChecked()throws Exception{
        assertThat(payments.ipn(Map.of("vnp_TxnRef",Long.toString(orderId))).get("RspCode")).isEqualTo("97");
        assertThat(payments.ipn(callback("00",1)).get("RspCode")).isEqualTo("04");
        var signed=callback("00",200000000);assertThat(payments.ipn(signed).get("RspCode")).isEqualTo("00");assertThat(payments.ipn(signed).get("RspCode")).isEqualTo("02");
        assertThat(orders.findById(orderId).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.PAID);assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);
    }
    @Test void failedPaymentRestoresStockOnlyOnce()throws Exception{
        var signed=callback("24",200000000);assertThat(payments.ipn(signed).get("RspCode")).isEqualTo("00");assertThat(payments.ipn(signed).get("RspCode")).isEqualTo("02");assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(2);assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
    }
    @Test void paymentLinkUsesSandboxAndUnpaidOrdersCannotShip(){
        var order=orders.findById(orderId).orElseThrow();assertThat(payments.paymentUrl(order,"127.0.0.1")).startsWith("https://sandbox.vnpayment.vn/").contains("vnp_SecureHash=");
        assertThatThrownBy(()->commerce.status(buyer.getEmail(),orderId,"CONFIRMED",true)).isInstanceOf(IllegalArgumentException.class);
    }
    /** Moves the order's creation time back so the 20-minute payment window has passed. */
    void age(long id,long minutes){var order=orders.findById(id).orElseThrow();order.setCreatedAt(java.util.Date.from(java.time.Instant.now().minus(java.time.Duration.ofMinutes(minutes))));orders.saveAndFlush(order);}
    long stock(){return products.findById(product.getId()).orElseThrow().getQuantity();}
    @Test void lateIpnAfterExpiryRequestsRefundWithoutReopeningTheOrder()throws Exception{
        age(orderId,25);payments.expire(orderId);
        var expired=orders.findById(orderId).orElseThrow();
        assertThat(expired.getStatus()).isEqualTo(OrderStatus.CANCELLED);assertThat(expired.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);
        long stockAfterExpiry=stock();assertThat(stockAfterExpiry).isEqualTo(2);

        var late=callback("00",200000000,Map.of("vnp_TransactionNo","14567890"));
        assertThat(payments.ipn(late).get("RspCode")).isEqualTo("00");
        var refund=orders.findById(orderId).orElseThrow();
        assertThat(refund.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUIRED);assertThat(refund.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(refund.getPaymentReference()).isEqualTo("14567890");
        assertThat(stock()).isEqualTo(stockAfterExpiry);
        assertThat(OrderTransitions.allowed(refund)).isEmpty();
        var view=commerce.detail(buyer.getEmail(),orderId,true);
        assertThat(view.refundRequired()).isTrue();assertThat(view.paymentReference()).isEqualTo("14567890");assertThat(view.allowedStatuses()).isEmpty();
    }
    @Test void repeatedLateIpnIsAcknowledgedAsAlreadyHandledAndChangesNothing()throws Exception{
        age(orderId,25);payments.expire(orderId);
        assertThat(payments.ipn(callback("00",200000000,Map.of("vnp_TransactionNo","111"))).get("RspCode")).isEqualTo("00");
        assertThat(payments.ipn(callback("00",200000000,Map.of("vnp_TransactionNo","222"))).get("RspCode")).isEqualTo("02");
        var order=orders.findById(orderId).orElseThrow();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUIRED);assertThat(order.getPaymentReference()).isEqualTo("111");
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);assertThat(stock()).isEqualTo(2);
    }
    @Test void lateFailureIpnAfterExpiryIsIgnoredBecauseNoMoneyMoved()throws Exception{
        age(orderId,25);payments.expire(orderId);
        assertThat(payments.ipn(callback("24",200000000)).get("RspCode")).isEqualTo("02");
        var order=orders.findById(orderId).orElseThrow();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.EXPIRED);assertThat(order.getPaymentReference()).isNull();assertThat(stock()).isEqualTo(2);
    }
    @Test void paymentAfterCustomerCancellationAlsoRequiresRefund()throws Exception{
        commerce.status(buyer.getEmail(),orderId,"CANCELLED",false);
        assertThat(payments.ipn(callback("00",200000000,Map.of("vnp_TransactionNo","333"))).get("RspCode")).isEqualTo("00");
        var order=orders.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.REFUND_REQUIRED);assertThat(stock()).isEqualTo(2);
    }
    @Test void refundRequiredOrdersCannotBeArchivedOutOfTheAdminQueue()throws Exception{
        age(orderId,25);payments.expire(orderId);payments.ipn(callback("00",200000000));
        assertThatThrownBy(()->commerce.archive(buyer.getEmail(),orderId,true)).isInstanceOf(IllegalArgumentException.class).hasMessageContaining("hoàn tiền");
    }
    @Test void ordinaryPaymentStoresTheVnpayTransactionNumber()throws Exception{
        assertThat(payments.ipn(callback("00",200000000,Map.of("vnp_TransactionNo","999"))).get("RspCode")).isEqualTo("00");
        var order=orders.findById(orderId).orElseThrow();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);assertThat(order.getPaymentReference()).isEqualTo("999");
    }
}
