package com.example.shop;

import com.example.shop.domain.*;
import com.example.shop.repository.*;
import com.example.shop.api.service.CommerceService;
import com.example.shop.service.Tokens;
import com.example.shop.support.MailLinks;
import com.example.shop.api.dto.CommerceRequests.*;
import com.fasterxml.jackson.databind.*;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.*;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.mock.web.MockHttpSession;
import java.util.*;
import java.util.concurrent.*;
import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class CommerceApiTests {
    @Test void cartAndCheckoutRejectStoppedHiddenAndSoldOutProductsWithoutChangingStock() {
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1));
        product.setActive(false); products.save(product);
        assertThat(commerce.getCart(buyer.getEmail()).items().get(0).active()).isFalse();
        assertThatThrownBy(() -> commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null))).isInstanceOf(IllegalArgumentException.class);
        product.setActive(true); product.setTechnologyCatalog(false); products.save(product);
        assertThat(commerce.getCart(buyer.getEmail()).items().get(0).active()).isFalse();
        assertThatThrownBy(() -> commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null))).isInstanceOf(IllegalArgumentException.class);
        product.setTechnologyCatalog(true); product.setQuantity(0); products.save(product);
        assertThat(commerce.getCart(buyer.getEmail()).items().get(0).stock()).isZero();
        assertThatThrownBy(() -> commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isZero();
        product.setQuantity(2); products.save(product);
        assertThat(commerce.getCart(buyer.getEmail()).items().get(0).active()).isTrue();
        commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null));
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);
    }
    @Autowired ProductVariantRepository variants;
    ProductVariant variant(String color,double price,long stock){
        var v=new ProductVariant();v.setProduct(product);v.setSku(UUID.randomUUID().toString());v.setPrice(price);v.setQuantity(stock);v.getOptions().put("Màu sắc",color);v.getOptions().put("Dung lượng","256 GB");return variants.save(v);
    }
    @Test void variantsRemainSeparateRepriceSnapshotAndRestoreExactlyOnce(){
        var black=variant("Đen",2000000,4);var white=variant("Trắng",3000000,6);
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),2,black.getId()));
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1,white.getId()));
        assertThat(commerce.getCart(buyer.getEmail()).items()).hasSize(2);
        black.setPrice(2500000);variants.save(black);
        var order=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null));
        assertThat(order.totalPrice()).isEqualTo(8000000);
        assertThat(order.items()).extracting(i->i.sku()).containsExactlyInAnyOrder(black.getSku(),white.getSku());
        assertThat(variants.findById(black.getId()).orElseThrow().getQuantity()).isEqualTo(2);
        assertThat(variants.findById(white.getId()).orElseThrow().getQuantity()).isEqualTo(5);
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(7);
        new TransactionTemplate(transactions).executeWithoutResult(s->{var v=variants.findById(black.getId()).orElseThrow();v.getOptions().put("Màu sắc","Xanh");});
        assertThat(commerce.detail(buyer.getEmail(),order.id(),false).items()).anyMatch(i->i.variantLabel().contains("Đen"));
        commerce.status(buyer.getEmail(),order.id(),"CANCELLED",false);
        commerce.status(buyer.getEmail(),order.id(),"CANCELLED",false);
        assertThat(variants.findById(black.getId()).orElseThrow().getQuantity()).isEqualTo(4);
        assertThat(variants.findById(white.getId()).orElseThrow().getQuantity()).isEqualTo(6);
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
    }
    @Test void variantSelectionCannotBeForgedOrSkipped(){
        var v=variant("Đen",2000000,2);
        assertThatThrownBy(()->commerce.add(buyer.getEmail(),new AddItem(product.getId(),1))).isInstanceOf(IllegalArgumentException.class);
        var second=new Product();second.setName("Other");second.setQuantity(2);second.setPrice(1000000);long secondId=products.save(second).getId();
        assertThatThrownBy(()->commerce.add(buyer.getEmail(),new AddItem(secondId,1,v.getId()))).hasMessageContaining("không thuộc");
        assertThatThrownBy(()->commerce.add(buyer.getEmail(),new AddItem(product.getId(),3,v.getId()))).isInstanceOf(IllegalArgumentException.class);
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1,v.getId()));
        v.setActive(false);variants.save(v);
        assertThatThrownBy(()->commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null))).isInstanceOf(IllegalArgumentException.class);
        assertThat(variants.findById(v.getId()).orElseThrow().getQuantity()).isEqualTo(2);
    }
    @Test void concurrentVariantCheckoutDoesNotOversell()throws Exception{
        var v=variant("Đen",2000000,1);product.setQuantity(1);products.save(product);
        for(var u:List.of(buyer,other))commerce.add(u.getEmail(),new AddItem(product.getId(),1,v.getId()));
        var start=new CountDownLatch(1);var pool=Executors.newFixedThreadPool(2);
        try{
            var futures=new ArrayList<Future<Boolean>>();
            for(var u:List.of(buyer,other))futures.add(pool.submit(()->{start.await();try{commerce.checkout(u.getEmail(),UUID.randomUUID().toString(),input(null));return true;}catch(IllegalArgumentException e){return false;}}));
            start.countDown();int success=0;for(var f:futures)if(f.get(20,TimeUnit.SECONDS))success++;
            assertThat(success).isEqualTo(1);assertThat(variants.findById(v.getId()).orElseThrow().getQuantity()).isZero();assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isZero();
        }finally{pool.shutdownNow();}
    }
    @Autowired MockMvc mvc; @Autowired ObjectMapper json; @Autowired UserRepository users; @Autowired RoleRepository roles;
    @Autowired ProductRepository products; @Autowired OrderRepository orders; @Autowired VoucherRepository vouchers;
    @Autowired PasswordEncoder encoder; @Autowired CommerceService commerce; @Autowired PlatformTransactionManager transactions;
    @Autowired CartRepository carts; @Autowired CategoryRepository categories;
    @MockitoBean JavaMailSender mail;
    User buyer,other,admin; Product product;
    final String password="Test-password-2026";
    @BeforeEach void setup(){
        buyer=createUser("USER");other=createUser("USER");admin=createUser("ADMIN");
        product=new Product();product.setName("Test laptop "+UUID.randomUUID());product.setPrice(2000000);product.setQuantity(10);product.setSku(UUID.randomUUID().toString());product=products.save(product);
        when(mail.createMimeMessage()).thenAnswer(i -> new jakarta.mail.internet.MimeMessage(jakarta.mail.Session.getInstance(new Properties())));
    }
    User createUser(String name){Role role=roles.findByName(name);if(role==null){role=new Role();role.setName(name);role=roles.save(role);}var u=new User();u.setEmail(UUID.randomUUID()+"@test.local");u.setFullName("Test Buyer");u.setPassword(encoder.encode(password));u.setEnabled(true);u.setRole(role);return users.save(u);}
    Checkout input(String voucher){return new Checkout("Test Buyer","123 Test Street","0909876543",voucher,"COD");}
    String body(Object value)throws Exception{return json.writeValueAsString(value);}
    JsonNode read(MvcResult result)throws Exception{return json.readTree(result.getResponse().getContentAsString());}
    void add(User u,long quantity)throws Exception{mvc.perform(post("/api/v1/cart/items").with(user(u.getEmail())).with(csrf()).contentType("application/json").content(body(new AddItem(product.getId(),quantity)))).andExpect(status().isOk());}
    JsonNode checkout(User u,String key,String voucher)throws Exception{return read(mvc.perform(post("/api/v1/orders").with(user(u.getEmail())).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(body(input(voucher)))).andExpect(status().isCreated()).andReturn());}
    @Test void securityAndLoginReturnJson()throws Exception{
        mvc.perform(get("/api/v1/cart")).andExpect(status().isUnauthorized()).andExpect(content().contentTypeCompatibleWith("application/json"));
        mvc.perform(get("/api/v1/admin/dashboard").with(user(buyer.getEmail()))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/cart/items").with(user(buyer.getEmail())).contentType("application/json").content(body(new AddItem(product.getId(),1)))).andExpect(status().isForbidden());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",buyer.getEmail(),"password","wrong")))).andExpect(status().isUnauthorized());
        var login=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",buyer.getEmail(),"password",password)))).andExpect(status().isOk()).andExpect(jsonPath("$.role").value("USER")).andReturn();
        var session=(MockHttpSession)login.getRequest().getSession();
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isOk());
        buyer.setIsLocked(true);users.save(buyer);
        mvc.perform(get("/api/v1/auth/me").session(session)).andExpect(status().isUnauthorized());
    }
    @Test void checkoutRepricesAppliesVoucherAndIsIdempotent()throws Exception{
        add(buyer,2);String code="TEST"+UUID.randomUUID().toString().substring(0,8).toUpperCase();var v=new Voucher();v.setCode(code);v.setDiscount(10);v.setAll(true);vouchers.save(v);
        product.setPrice(2500000);products.save(product);
        String key=UUID.randomUUID().toString();var first=checkout(buyer,key,code);var second=checkout(buyer,key,code);
        assertThat(first.get("id")).isEqualTo(second.get("id"));assertThat(first.get("totalPrice").asDouble()).isEqualTo(4500000);assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(8);
        mvc.perform(get("/api/v1/cart").with(user(buyer.getEmail()))).andExpect(jsonPath("$.items.length()").value(0));
        product=products.findById(product.getId()).orElseThrow();product.setName("Changed name");products.save(product);
        mvc.perform(get("/api/v1/orders/"+first.get("id")).with(user(buyer.getEmail()))).andExpect(jsonPath("$.items[0].name").value(org.hamcrest.Matchers.startsWith("Test laptop")));
        mvc.perform(post("/api/v1/orders").with(user(buyer.getEmail())).with(csrf()).header("Idempotency-Key",key).contentType("application/json").content(body(input(null)))).andExpect(status().isBadRequest());
    }
    @Test void ownershipAndCancellationRestoreStockOnlyOnce()throws Exception{
        add(buyer,2);var order=checkout(buyer,UUID.randomUUID().toString(),null);long id=order.get("id").asLong();
        mvc.perform(get("/api/v1/orders/"+id).with(user(other.getEmail()))).andExpect(status().isNotFound());
        for(int i=0;i<2;i++)mvc.perform(patch("/api/v1/orders/"+id+"/status").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content("{\"status\":\"CANCELLED\"}")).andExpect(status().isOk());
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        mvc.perform(patch("/api/v1/admin/orders/"+id+"/status").with(user(admin.getEmail()).roles("ADMIN")).with(csrf()).contentType("application/json").content("{\"status\":\"COMPLETED\"}")).andExpect(status().isBadRequest());
    }
    @Test void invalidVoucherAndStockRollback()throws Exception{
        add(buyer,2);
        mvc.perform(post("/api/v1/orders").with(user(buyer.getEmail())).with(csrf()).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body(input("NOT-A-VOUCHER")))).andExpect(status().isBadRequest());
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        product.setQuantity(1);products.save(product);
        mvc.perform(post("/api/v1/orders").with(user(buyer.getEmail())).with(csrf()).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body(input(null)))).andExpect(status().isBadRequest());
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(1);
        mvc.perform(post("/api/v1/cart/items").with(user(other.getEmail())).with(csrf()).contentType("application/json").content(body(new AddItem(product.getId(),0)))).andExpect(status().isBadRequest());
    }
    @Test void adminFulfillmentAndPurchasedReview()throws Exception{
        add(buyer,1);long id=checkout(buyer,UUID.randomUUID().toString(),null).get("id").asLong();
        String summary="/api/v1/products/"+product.getId()+"/reviews/summary";
        mvc.perform(get(summary).with(user(buyer.getEmail()))).andExpect(jsonPath("$.canReview").value(false));
        mvc.perform(get("/api/v1/admin/orders/"+id).with(user(admin.getEmail()).roles("ADMIN"))).andExpect(jsonPath("$.allowedStatuses").value(org.hamcrest.Matchers.contains("CONFIRMED","CANCELLED")));
        mvc.perform(patch("/api/v1/admin/orders/"+id+"/status").with(user(admin.getEmail()).roles("ADMIN")).with(csrf()).contentType("application/json").content(body(Map.of("status","COMPLETED")))).andExpect(status().isBadRequest());
        for(String s:List.of("CONFIRMED","SHIPPING","COMPLETED"))mvc.perform(patch("/api/v1/admin/orders/"+id+"/status").with(user(admin.getEmail()).roles("ADMIN")).with(csrf()).contentType("application/json").content(body(Map.of("status",s)))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/orders/"+id).with(user(buyer.getEmail()))).andExpect(jsonPath("$.paymentStatus").value("PAID"));
        mvc.perform(get(summary).with(user(buyer.getEmail()))).andExpect(jsonPath("$.canReview").value(true));
        mvc.perform(post("/api/v1/products/"+product.getId()+"/reviews").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(new ReviewInput(6,"Sai số sao")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/products/"+product.getId()+"/reviews").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(new ReviewInput(5,"Sản phẩm tốt")))).andExpect(status().isCreated());
        mvc.perform(post("/api/v1/products/"+product.getId()+"/reviews").with(user(other.getEmail())).with(csrf()).contentType("application/json").content(body(new ReviewInput(5,"Không mua")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/products/"+product.getId()+"/reviews").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(new ReviewInput(4,"Trùng")))).andExpect(status().isBadRequest());
        mvc.perform(get(summary).with(user(buyer.getEmail()))).andExpect(jsonPath("$.average").value(5.0)).andExpect(jsonPath("$.count").value(1)).andExpect(jsonPath("$.canReview").value(false));
        var reviews=read(mvc.perform(get("/api/v1/products/"+product.getId()+"/reviews").with(user(buyer.getEmail()))).andExpect(jsonPath("$[0].editable").value(true)).andReturn());
        String reviewPath="/api/v1/products/"+product.getId()+"/reviews/"+reviews.get(0).get("id").asLong();
        mvc.perform(put(reviewPath).with(user(other.getEmail())).with(csrf()).contentType("application/json").content(body(new ReviewInput(1,"Not mine")))).andExpect(status().isNotFound());
        mvc.perform(delete(reviewPath).with(user(other.getEmail())).with(csrf())).andExpect(status().isNotFound());
        mvc.perform(put(reviewPath).with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(new ReviewInput(4,"Đã cập nhật")))).andExpect(status().isOk()).andExpect(jsonPath("$.rating").value(4));
        mvc.perform(delete(reviewPath).with(user(buyer.getEmail())).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get(summary)).andExpect(jsonPath("$.average").value(0.0)).andExpect(jsonPath("$.count").value(0)).andExpect(jsonPath("$.canReview").value(false));
        mvc.perform(get("/api/v1/admin/orders/"+id+"/invoice").with(user(admin.getEmail()).roles("ADMIN"))).andExpect(status().isOk()).andExpect(content().contentType("application/pdf"));
        mvc.perform(delete("/api/v1/admin/orders/"+id).with(user(admin.getEmail()).roles("ADMIN")).with(csrf())).andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/orders/"+id).with(user(buyer.getEmail()))).andExpect(status().isOk()).andExpect(jsonPath("$.items.length()").value(1));
        assertThat(orders.findById(id).orElseThrow().isAdminArchived()).isTrue();
        mvc.perform(post("/api/v1/admin/orders/"+id+"/restore").with(user(admin.getEmail()).roles("ADMIN")).with(csrf())).andExpect(status().isOk()).andExpect(jsonPath("$.archived").value(false));
    }
    @Test void simultaneousBuyersCannotOversell()throws Exception{
        product.setQuantity(1);products.save(product);add(buyer,1);add(other,1);
        var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try{
            List<Future<Boolean>> futures=new ArrayList<>();for(User u:List.of(buyer,other))futures.add(executor.submit(()->{start.await();try{commerce.checkout(u.getEmail(),UUID.randomUUID().toString(),input(null));return true;}catch(IllegalArgumentException e){return false;}}));
            start.countDown();int successes=0;for(var future:futures)if(future.get(20,TimeUnit.SECONDS))successes++;
            assertThat(successes).isEqualTo(1);assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isZero();
        }finally{executor.shutdownNow();}
    }
    @Test void selectedCheckoutLeavesOtherItemsInCart()throws Exception{
        add(buyer,1);var second=new Product();second.setName("Another item");second.setPrice(500000);second.setQuantity(4);second=products.save(second);
        commerce.add(buyer.getEmail(),new AddItem(second.getId(),1));var cart=commerce.getCart(buyer.getEmail());
        var selected=cart.items().stream().filter(i->i.productId()==product.getId()).findFirst().orElseThrow();
        var quote=commerce.quote(buyer.getEmail(),"",List.of(selected.id()));assertThat(quote.totalPrice()).isEqualTo(2000000);
        commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),new Checkout("Test Buyer","Test address","0909876543",null,"COD",List.of(selected.id())));
        assertThat(commerce.getCart(buyer.getEmail()).items()).hasSize(1);assertThat(commerce.getCart(buyer.getEmail()).items().get(0).productId()).isEqualTo(second.getId());
    }
    @Test void registerVerifyAndResetPassword()throws Exception{
        String email=UUID.randomUUID()+"@test.local";
        mvc.perform(post("/api/v1/auth/register").with(csrf()).contentType("application/json").content(body(Map.of("firstName","Test","lastName","User","email",email,"password",password,"confirmPassword",password)))).andExpect(status().isCreated());
        var u=users.findByEmail(email);assertThat(u.isEnabled()).isFalse();assertThat(u.getPassword()).isNotEqualTo(password);
        // Only the SHA-256 of the mailed code is stored, so the stored value cannot be replayed.
        String code=MailLinks.lastParameter(mail,"code");
        assertThat(u.getVerificationCode()).isEqualTo(Tokens.hash(code)).isNotEqualTo(code);
        mvc.perform(post("/api/v1/auth/verify").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("code",u.getVerificationCode())))).andExpect(jsonPath("$.verified").value(false));
        mvc.perform(post("/api/v1/auth/verify").with(csrf()).contentType("application/json").content(json.writeValueAsString(Map.of("code",code)))).andExpect(jsonPath("$.verified").value(true));
        mvc.perform(post("/api/v1/auth/forgot-password").with(csrf()).contentType("application/json").content(body(Map.of("email",email)))).andExpect(status().isOk());
        u=users.findByEmail(email);String token=MailLinks.lastParameter(mail,"token");
        assertThat(u.getResetPasswordToken()).isEqualTo(Tokens.hash(token)).isNotEqualTo(token);
        var stored=Map.of("token",u.getResetPasswordToken(),"password","New-password-2026","confirmPassword","New-password-2026");
        mvc.perform(post("/api/v1/auth/reset-password").with(csrf()).contentType("application/json").content(body(stored))).andExpect(status().isNotFound());
        // A token that was stored in plain text before hashing was introduced no longer works.
        var legacy=users.findByEmail(email);legacy.setResetPasswordToken("legacy-plain-token");users.save(legacy);
        mvc.perform(post("/api/v1/auth/reset-password").with(csrf()).contentType("application/json").content(body(Map.of("token","legacy-plain-token","password","New-password-2026","confirmPassword","New-password-2026")))).andExpect(status().isNotFound());
        legacy.setResetPasswordToken(Tokens.hash(token));users.save(legacy);
        var reset=Map.of("token",token,"password","New-password-2026","confirmPassword","New-password-2026");
        mvc.perform(post("/api/v1/auth/reset-password").with(csrf()).contentType("application/json").content(body(reset))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/reset-password").with(csrf()).contentType("application/json").content(body(reset))).andExpect(status().isNotFound());
        verify(mail,times(2)).send(any(jakarta.mail.internet.MimeMessage.class));
    }
    @Test void voucherValidityMinimumAndDiscountCap()throws Exception{
        add(buyer,1);
        var v=new Voucher();v.setCode("RULE-"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT));v.setAll(true);v.setDiscount(10);v=vouchers.save(v);
        final String code=v.getCode();
        v.setActive(false);vouchers.save(v);
        assertThatThrownBy(()->commerce.quote(buyer.getEmail(),code,null)).hasMessageContaining("ngừng hoạt động");
        v.setActive(true);v.setStartDate(new Date(System.currentTimeMillis()+3600000));vouchers.save(v);
        assertThatThrownBy(()->commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(code))).hasMessageContaining("chưa đến");
        v.setStartDate(null);v.setEndDate(new Date(System.currentTimeMillis()-1000));vouchers.save(v);
        assertThatThrownBy(()->commerce.quote(buyer.getEmail(),code,null)).hasMessageContaining("hết hạn");
        v.setEndDate(null);v.setMinimumOrder(3000000);vouchers.save(v);
        assertThatThrownBy(()->commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(code))).hasMessageContaining("tối thiểu");
        assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(10);
        assertThat(vouchers.findById(v.getId()).orElseThrow().getUsedCount()).isZero();
        v.setMinimumOrder(2000000);v.setMaximumDiscount(50000d);v.setUsageLimit(1L);vouchers.save(v);
        assertThat(commerce.quote(buyer.getEmail(),code,null).discountAmount()).isEqualTo(50000);
        String key=UUID.randomUUID().toString();
        var order=commerce.checkout(buyer.getEmail(),key,input(code));
        assertThat(order.totalPrice()).isEqualTo(1950000);
        assertThat(commerce.checkout(buyer.getEmail(),key,input(code)).id()).isEqualTo(order.id());
        assertThat(vouchers.findById(v.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
        add(other,1);
        assertThatThrownBy(()->commerce.checkout(other.getEmail(),UUID.randomUUID().toString(),input(code))).hasMessageContaining("hết lượt");
    }
    @Autowired com.example.shop.api.service.SandboxPaymentService payments;
    Voucher limitedVoucher(String prefix){
        var v=new Voucher();v.setCode(prefix+UUID.randomUUID().toString().toUpperCase(Locale.ROOT));v.setAll(true);v.setDiscount(10);v.setUsageLimit(1L);return vouchers.save(v);
    }
    long used(Voucher v){return vouchers.findById(v.getId()).orElseThrow().getUsedCount();}
    @Test void cancellingAnOrderGivesTheVoucherUseBackExactlyOnce()throws Exception{
        add(buyer,1);add(other,1);
        var v=limitedVoucher("REFUND-");final String code=v.getCode();
        var order=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(code));
        assertThat(used(v)).isEqualTo(1);
        assertThatThrownBy(()->commerce.checkout(other.getEmail(),UUID.randomUUID().toString(),input(code))).hasMessageContaining("hết lượt");
        commerce.status(buyer.getEmail(),order.id(),"CANCELLED",false);
        assertThat(used(v)).isZero();
        commerce.status(buyer.getEmail(),order.id(),"CANCELLED",false);
        assertThat(used(v)).isZero();
        commerce.checkout(other.getEmail(),UUID.randomUUID().toString(),input(code));
        assertThat(used(v)).isEqualTo(1);
    }
    @Test void anExpiredOnlineOrderGivesTheVoucherUseBack()throws Exception{
        add(buyer,1);
        var v=limitedVoucher("EXPIRE-");
        var order=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(v.getCode()));
        assertThat(used(v)).isEqualTo(1);
        var stored=orders.findById(order.id()).orElseThrow();stored.setPaymentMethod("VNPAY");stored.setCreatedAt(new Date(System.currentTimeMillis()-3600000));orders.save(stored);
        payments.expire(order.id());
        assertThat(orders.findById(order.id()).orElseThrow().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        assertThat(used(v)).isZero();
        payments.expire(order.id());
        assertThat(used(v)).isZero();
    }
    @Test void aDeletedVoucherDoesNotBlockCancellingItsOrder()throws Exception{
        add(buyer,1);
        var v=limitedVoucher("GONE-");
        var order=commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(v.getCode()));
        vouchers.deleteById(v.getId());
        assertThat(commerce.status(buyer.getEmail(),order.id(),"CANCELLED",false).status()).isEqualTo("CANCELLED");
    }
    @Test void popularSortRanksByConfirmedUnitsNotByPendingOrders()throws Exception{
        String key=UUID.randomUUID().toString();
        product.setName(key+" pending");products.save(product);
        var hot=new Product();hot.setName(key+" hot");hot.setPrice(1000000);hot.setQuantity(10);hot.setSku(UUID.randomUUID().toString());hot=products.save(hot);
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),5));
        commerce.checkout(buyer.getEmail(),UUID.randomUUID().toString(),input(null));
        commerce.add(other.getEmail(),new AddItem(hot.getId(),1));
        var confirmed=commerce.checkout(other.getEmail(),UUID.randomUUID().toString(),input(null));
        commerce.status(admin.getEmail(),confirmed.id(),"CONFIRMED",true);
        mvc.perform(get("/api/v1/products").param("search",key).param("sort","popular")).andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].name").value(key+" hot")).andExpect(jsonPath("$.content[1].name").value(key+" pending"));
    }
    @Test void cartLineRemembersThePriceWhenItWasAddedSoTheShopperSeesAChange(){
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1));
        product.setPrice(2500000);products.save(product);
        var line=commerce.getCart(buyer.getEmail()).items().get(0);
        assertThat(line.addedPrice()).isEqualTo(2000000);
        assertThat(line.price()).isEqualTo(2500000);
    }
    @Test void simultaneousVoucherUseHonorsLimit()throws Exception{
        add(buyer,1);add(other,1);
        var v=new Voucher();v.setCode("LIMIT-"+UUID.randomUUID().toString().toUpperCase(Locale.ROOT));v.setAll(true);v.setDiscount(10);v.setUsageLimit(1L);v=vouchers.save(v);
        final String code=v.getCode();var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try{
            var futures=new ArrayList<Future<Boolean>>();
            for(User u:List.of(buyer,other))futures.add(executor.submit(()->{start.await();try{commerce.checkout(u.getEmail(),UUID.randomUUID().toString(),input(code));return true;}catch(IllegalArgumentException e){return false;}}));
            start.countDown();int success=0;for(var f:futures)if(f.get(20,TimeUnit.SECONDS))success++;
            assertThat(success).isEqualTo(1);
            assertThat(vouchers.findById(v.getId()).orElseThrow().getUsedCount()).isEqualTo(1);
            assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(9);
        }finally{executor.shutdownNow();}
    }
    @Test void orderNotesAreValidatedAndVisibleToOwnerAndAdmin()throws Exception{
        add(buyer,1);
        var request=new Checkout("Test Buyer","Test address","0909876543",null,"COD",null,"Gọi trước khi giao");
        var result=mvc.perform(post("/api/v1/orders").with(user(buyer.getEmail())).with(csrf()).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body(request)))
            .andExpect(status().isCreated()).andExpect(jsonPath("$.note").value(request.note())).andReturn();
        long id=read(result).get("id").asLong();
        mvc.perform(get("/api/v1/admin/orders/"+id).with(user(admin.getEmail()).roles("ADMIN"))).andExpect(jsonPath("$.note").value(request.note()));
        var invalid=new Checkout("Test Buyer","Test address","0909876543",null,"COD",null,"x".repeat(1001));
        mvc.perform(post("/api/v1/orders").with(user(buyer.getEmail())).with(csrf()).header("Idempotency-Key",UUID.randomUUID().toString()).contentType("application/json").content(body(invalid))).andExpect(status().isBadRequest());
    }
    @Test void simultaneousRetriesCreateOneOrder()throws Exception{
        add(buyer,1);String key=UUID.randomUUID().toString();var start=new CountDownLatch(1);var executor=Executors.newFixedThreadPool(2);
        try{
            var first=executor.submit(()->{start.await();return commerce.checkout(buyer.getEmail(),key,input(null)).id();});
            var second=executor.submit(()->{start.await();return commerce.checkout(buyer.getEmail(),key,input(null)).id();});
            start.countDown();assertThat(first.get(20,TimeUnit.SECONDS)).isEqualTo(second.get(20,TimeUnit.SECONDS));
            assertThat(products.findById(product.getId()).orElseThrow().getQuantity()).isEqualTo(9);
        }finally{executor.shutdownNow();}
    }
    @Test void profileAndPasswordRequireCurrentCredentials()throws Exception{
        var profile=new Profile("Tên mới","0909876543","Địa chỉ mới");
        mvc.perform(put("/api/v1/account").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(profile))).andExpect(status().isOk());
        mvc.perform(get("/api/v1/account").with(user(buyer.getEmail()))).andExpect(jsonPath("$.fullName").value("Tên mới"));
        mvc.perform(get("/api/v1/account").with(user(other.getEmail()))).andExpect(jsonPath("$.fullName").value("Test Buyer"));
        mvc.perform(post("/api/v1/account/password").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(new Password("wrong","Changed-pass-2026")))).andExpect(status().isBadRequest());
        mvc.perform(post("/api/v1/account/password").with(user(buyer.getEmail())).with(csrf()).contentType("application/json").content(body(new Password(password,"Changed-pass-2026")))).andExpect(status().isOk());
        mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",buyer.getEmail(),"password",password)))).andExpect(status().isUnauthorized());
        var logged=mvc.perform(post("/api/v1/auth/login").with(csrf()).contentType("application/json").content(body(Map.of("email",buyer.getEmail(),"password","Changed-pass-2026")))).andExpect(status().isOk()).andReturn();
        var session=(MockHttpSession)logged.getRequest().getSession();
        mvc.perform(post("/api/v1/auth/logout").session(session).with(csrf())).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
        mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized());
    }

    // --- Read paths take no row locks (M1) ---
    @Test void cartQuoteAndOrderReadsDoNotWaitForRowLocksHeldByWriters()throws Exception{
        add(buyer,1);
        var v=new Voucher();String code="READ-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);v.setCode(code);v.setDiscount(10);v.setAll(true);vouchers.save(v);
        var locked=new CountDownLatch(1);var release=new CountDownLatch(1);
        var holder=Executors.newSingleThreadExecutor();var readers=Executors.newFixedThreadPool(4);
        try{
            // A writer (as checkout would) holds FOR UPDATE locks on the buyer and on the voucher.
            holder.submit(()->new TransactionTemplate(transactions).execute(status->{
                users.lockByEmail(buyer.getEmail());vouchers.lockByCode(code);locked.countDown();
                try{release.await(30,TimeUnit.SECONDS);}catch(InterruptedException e){Thread.currentThread().interrupt();}
                return null;
            }));
            assertThat(locked.await(10,TimeUnit.SECONDS)).isTrue();
            var reads=List.of(
                readers.submit(()->commerce.getCart(buyer.getEmail())),
                readers.submit(()->commerce.quote(buyer.getEmail(),code,null)),
                readers.submit(()->commerce.quote(buyer.getEmail(),code,null)),
                readers.submit(()->commerce.list(buyer.getEmail())));
            // The lock timeout of the test database is 10 s, so a read that queues on a lock fails here.
            for(var read:reads)read.get(5,TimeUnit.SECONDS);
        }finally{release.countDown();holder.shutdownNow();readers.shutdownNow();}
    }
    @Test void emptyCartReadDoesNotCreateACartRow(){
        var fresh=createUser("USER");
        assertThat(commerce.getCart(fresh.getEmail()).items()).isEmpty();
        assertThat(carts.findByUser(fresh)).isNull();
        assertThatThrownBy(()->commerce.quote(fresh.getEmail(),"",null)).hasMessageContaining("Giỏ hàng trống");
    }

    // --- Money rules as they behave today. They wait for the project owner's decision (docs/API.md, "Quy tắc tiền"). ---
    @ParameterizedTest(name="minimumOrder counts the whole selected subtotal: other line {0} -> voucher applies={1}")
    @CsvSource({"3999999,false","4000000,true","4500000,true"})
    void voucherMinimumOrderIsComparedWithTheWholeSelectedSubtotalNotTheEligibleLines(long otherLinePrice,boolean applies){
        var laptops=new Category();laptops.setName("Laptop "+UUID.randomUUID());laptops=categories.save(laptops);
        product.setCategory(laptops);product.setPrice(1000000);products.save(product);
        var extra=new Product();extra.setName("Other line "+UUID.randomUUID());extra.setPrice(otherLinePrice);extra.setQuantity(3);extra=products.save(extra);
        var v=new Voucher();String code="MIN-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);
        v.setCode(code);v.setDiscount(10);v.setScopeType(ScopeType.CATEGORY);v.setCategories(new ArrayList<>(List.of(laptops)));v.setMinimumOrder(5000000);vouchers.save(v);
        commerce.add(buyer.getEmail(),new AddItem(product.getId(),1));commerce.add(buyer.getEmail(),new AddItem(extra.getId(),1));
        if(applies){
            var quote=commerce.quote(buyer.getEmail(),code,null);
            // 10 % of the eligible laptop line only (1,000,000), while the 5,000,000 minimum was met by the other line.
            assertThat(quote.discountAmount()).isEqualTo(100000);assertThat(quote.subtotal()).isEqualTo(1000000+otherLinePrice);
        }else assertThatThrownBy(()->commerce.quote(buyer.getEmail(),code,null)).hasMessageContaining("tối thiểu");
    }
    @ParameterizedTest(name="delivery is free at every basket size: price {0}, voucher {1}% -> shipping {2}, total {3}")
    @CsvSource({"500000,0,0,500000","999999,0,0,999999","1000000,0,0,1000000","1050000,10,0,945000","900000,10,0,810000"})
    void deliveryIsFreeWhateverTheBasketIsWorth(long price,double percent,double shipping,double total)throws Exception{
        product.setPrice(price);products.save(product);add(buyer,1);
        String code="";
        if(percent>0){var v=new Voucher();code="SHIP-"+UUID.randomUUID().toString().substring(0,8).toUpperCase(Locale.ROOT);v.setCode(code);v.setDiscount(percent);v.setAll(true);vouchers.save(v);}
        var quote=commerce.quote(buyer.getEmail(),code,null);
        assertThat(quote.shippingFee()).isEqualTo(shipping);assertThat(quote.totalPrice()).isEqualTo(total);
    }
}
