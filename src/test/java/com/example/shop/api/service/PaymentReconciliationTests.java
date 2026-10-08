package com.example.shop.api.service;
import com.example.shop.api.dto.CommerceRequests.*;
import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import java.nio.charset.StandardCharsets;
import java.util.*;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import static org.assertj.core.api.Assertions.*;
import static org.springframework.http.HttpMethod.POST;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.*;
import static org.springframework.test.web.client.response.MockRestResponseCreators.*;

/** querydr reconciliation for orders whose IPN never arrived. The gateway is mocked; no test reaches VNPay. */
@SpringBootTest(properties={"shop.vnpay.enabled=true","shop.vnpay.tmn-code=TESTONLY","shop.vnpay.secret=local-test-signing-key"}) @ActiveProfiles("test")
class PaymentReconciliationTests {
    static final String SECRET="local-test-signing-key",URL="http://vnpay.test/transaction";
    static MockRestServiceServer gateway;
    @TestConfiguration static class GatewayConfig {
        @Bean @Primary VnpayQueryClient mockedQueryClient(){
            var builder=RestClient.builder();gateway=MockRestServiceServer.bindTo(builder).build();
            return new VnpayQueryClient(builder.build(),URL);
        }
    }
    @Autowired CommerceService commerce;@Autowired SandboxPaymentService payments;@Autowired UserRepository users;@Autowired RoleRepository roles;@Autowired ProductRepository products;@Autowired OrderRepository orders;
    User buyer;Product product;long orderId;
    @BeforeEach void setup(){
        gateway.reset();
        var role=roles.findByName("USER");if(role==null){role=new Role();role.setName("USER");role=roles.save(role);}
        buyer=new User();buyer.setEmail(UUID.randomUUID()+"@test.local");buyer.setRole(role);buyer.setEnabled(true);buyer=users.save(buyer);
        product=new Product();product.setName("Reconciliation test product");product.setPrice(2000000);product.setQuantity(2);product=products.save(product);
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1));
        orderId=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),new Checkout("Test","Test address","0909876543",null,"VNPAY")).id();
    }
    long stock(){return products.findById(product.getId()).orElseThrow().getQuantity();}
    static String hmac(String data)throws Exception{var mac=Mac.getInstance("HmacSHA512");mac.init(new SecretKeySpec(SECRET.getBytes(StandardCharsets.UTF_8),"HmacSHA512"));return HexFormat.of().formatHex(mac.doFinal(data.getBytes(StandardCharsets.UTF_8)));}
    /** Builds a querydr answer in the field order VNPay signs it with. */
    String answer(String responseCode,String transactionStatus,long amount,String transactionNo,boolean sign)throws Exception{
        return answer(responseCode,transactionStatus,amount,transactionNo,sign,Long.toString(orderId));
    }
    String answer(String responseCode,String transactionStatus,long amount,String transactionNo,boolean sign,String txnRef)throws Exception{
        var fields=new LinkedHashMap<String,String>();
        fields.put("vnp_ResponseId",UUID.randomUUID().toString().replace("-",""));fields.put("vnp_Command","querydr");
        fields.put("vnp_ResponseCode",responseCode);fields.put("vnp_Message","Success");fields.put("vnp_TmnCode","TESTONLY");
        fields.put("vnp_TxnRef",txnRef);fields.put("vnp_Amount",Long.toString(amount));fields.put("vnp_BankCode","NCB");
        fields.put("vnp_PayDate","20261004120000");fields.put("vnp_TransactionNo",transactionNo);fields.put("vnp_TransactionType","01");
        fields.put("vnp_TransactionStatus",transactionStatus);fields.put("vnp_OrderInfo","Thanh toan don hang "+txnRef);fields.put("vnp_PromotionCode","");fields.put("vnp_PromotionAmount","");
        fields.put("vnp_SecureHash",sign?hmac(String.join("|",fields.values())):"f".repeat(128));
        return new ObjectMapper().writeValueAsString(fields);
    }
    void expectQuery(String body){gateway.expect(requestTo(URL)).andExpect(method(POST)).andExpect(jsonPath("$.vnp_Command").value("querydr")).andExpect(jsonPath("$.vnp_TxnRef").value(Long.toString(orderId))).andExpect(jsonPath("$.vnp_SecureHash").exists()).andRespond(withSuccess(body,org.springframework.http.MediaType.APPLICATION_JSON));}
    @Test void aPaymentWhoseIpnWasLostIsRecoveredByReconciliation()throws Exception{
        expectQuery(answer("00","00",200000000,"8812",true));
        var result=payments.query(orderId);
        assertThat(result.verified()).isTrue();assertThat(result.transactionStatus()).isEqualTo("00");assertThat(result.amount()).isEqualTo(200000000);
        var applied=payments.settle(orderId,result);
        gateway.verify();
        assertThat(applied.get("applied")).isEqualTo("00");assertThat(applied.get("paymentStatus")).isEqualTo("PAID");
        assertThat(applied.get("paymentReference")).isEqualTo("8812");
        var order=orders.findById(orderId).orElseThrow();
        assertThat(order.getPaymentStatus()).isEqualTo(PaymentStatus.PAID);assertThat(order.getStatus()).isEqualTo(OrderStatus.PENDING);
        assertThat(stock()).isEqualTo(1);
    }
    @Test void reconciliationOfAFailedTransactionReleasesTheStock()throws Exception{
        expectQuery(answer("00","02",200000000,"8813",true));
        var applied=payments.settle(orderId,payments.query(orderId));
        assertThat(applied.get("applied")).isEqualTo("00");assertThat(applied.get("paymentStatus")).isEqualTo("FAILED");
        var order=orders.findById(orderId).orElseThrow();
        assertThat(order.getStatus()).isEqualTo(OrderStatus.CANCELLED);assertThat(order.getPaymentReference()).isNull();
        assertThat(stock()).isEqualTo(2);
    }
    @Test void aTransactionStillInFlightIsLeftUntouched()throws Exception{
        expectQuery(answer("00","01",200000000,"",true));
        var applied=payments.settle(orderId,payments.query(orderId));
        assertThat(applied.get("applied")).isNull();assertThat(applied.get("paymentStatus")).isEqualTo("UNPAID");
        assertThat(orders.findById(orderId).orElseThrow().getStatus()).isEqualTo(OrderStatus.PENDING);assertThat(stock()).isEqualTo(1);
    }
    @Test void anUnsignedGatewayAnswerIsNotTrusted()throws Exception{
        expectQuery(answer("00","00",200000000,"8814",false));
        var result=payments.query(orderId);
        assertThat(result.verified()).isFalse();assertThat(result.message()).isEqualTo("Chữ ký phản hồi đối soát không hợp lệ");
        var applied=payments.settle(orderId,result);
        assertThat(applied.get("applied")).isNull();
        assertThat(orders.findById(orderId).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);assertThat(stock()).isEqualTo(1);
    }
    @Test void anUnreachableGatewayChangesNothing(){
        gateway.expect(requestTo(URL)).andRespond(withServerError());
        var result=payments.query(orderId);
        assertThat(result.verified()).isFalse();assertThat(result.message()).isEqualTo("Không kết nối được VNPay để đối soát");
        assertThat(payments.settle(orderId,result).get("applied")).isNull();
        assertThat(orders.findById(orderId).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);assertThat(stock()).isEqualTo(1);
    }
    @Test void anAnswerForAnotherOrderIsRejected()throws Exception{
        var foreign=answer("00","00",200000000,"8815",true,Long.toString(orderId+1000));
        gateway.expect(requestTo(URL)).andRespond(withSuccess(foreign,org.springframework.http.MediaType.APPLICATION_JSON));
        assertThat(payments.query(orderId).verified()).isFalse();
        assertThat(orders.findById(orderId).orElseThrow().getPaymentStatus()).isEqualTo(PaymentStatus.UNPAID);
    }
}
